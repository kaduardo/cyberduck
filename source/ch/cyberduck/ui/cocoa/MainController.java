package ch.cyberduck.ui.cocoa;

/*
 *  Copyright (c) 2005 David Kocher. All rights reserved.
 *  http://cyberduck.ch/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

import ch.cyberduck.core.*;
import ch.cyberduck.core.aquaticprime.Donation;
import ch.cyberduck.core.aquaticprime.License;
import ch.cyberduck.core.aquaticprime.LicenseFactory;
import ch.cyberduck.core.i18n.Locale;
import ch.cyberduck.core.importer.CrossFtpBookmarkCollection;
import ch.cyberduck.core.importer.FetchBookmarkCollection;
import ch.cyberduck.core.importer.FilezillaBookmarkCollection;
import ch.cyberduck.core.importer.FireFtpBookmarkCollection;
import ch.cyberduck.core.importer.FlowBookmarkCollection;
import ch.cyberduck.core.importer.InterarchyBookmarkCollection;
import ch.cyberduck.core.importer.ThirdpartyBookmarkCollection;
import ch.cyberduck.core.importer.TransmitBookmarkCollection;
import ch.cyberduck.core.local.Application;
import ch.cyberduck.core.local.ApplicationLauncherFactory;
import ch.cyberduck.core.local.Local;
import ch.cyberduck.core.local.LocalFactory;
import ch.cyberduck.core.serializer.HostReaderFactory;
import ch.cyberduck.core.serializer.ProfileReaderFactory;
import ch.cyberduck.core.sparkle.Updater;
import ch.cyberduck.core.threading.AbstractBackgroundAction;
import ch.cyberduck.core.threading.DefaultMainAction;
import ch.cyberduck.core.transfer.TransferCollection;
import ch.cyberduck.core.transfer.download.DownloadTransfer;
import ch.cyberduck.core.transfer.upload.UploadTransfer;
import ch.cyberduck.core.urlhandler.SchemeHandlerFactory;
import ch.cyberduck.ui.cocoa.application.*;
import ch.cyberduck.ui.cocoa.delegate.ArchiveMenuDelegate;
import ch.cyberduck.ui.cocoa.delegate.BookmarkMenuDelegate;
import ch.cyberduck.ui.cocoa.delegate.CopyURLMenuDelegate;
import ch.cyberduck.ui.cocoa.delegate.EditMenuDelegate;
import ch.cyberduck.ui.cocoa.delegate.HistoryMenuDelegate;
import ch.cyberduck.ui.cocoa.delegate.OpenURLMenuDelegate;
import ch.cyberduck.ui.cocoa.delegate.RendezvousMenuDelegate;
import ch.cyberduck.ui.cocoa.delegate.URLMenuDelegate;
import ch.cyberduck.ui.cocoa.foundation.NSAppleEventDescriptor;
import ch.cyberduck.ui.cocoa.foundation.NSAppleEventManager;
import ch.cyberduck.ui.cocoa.foundation.NSArray;
import ch.cyberduck.ui.cocoa.foundation.NSAttributedString;
import ch.cyberduck.ui.cocoa.foundation.NSBundle;
import ch.cyberduck.ui.cocoa.foundation.NSDictionary;
import ch.cyberduck.ui.cocoa.foundation.NSNotification;
import ch.cyberduck.ui.cocoa.foundation.NSNotificationCenter;
import ch.cyberduck.ui.cocoa.foundation.NSObject;
import ch.cyberduck.ui.cocoa.resources.IconCache;
import ch.cyberduck.ui.growl.Growl;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.rococoa.Foundation;
import org.rococoa.ID;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSInteger;
import org.rococoa.cocoa.foundation.NSRect;
import org.rococoa.cocoa.foundation.NSUInteger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Setting the main menu and implements application delegate methods
 *
 * @version $Id: MainController.java 10709 2012-12-22 19:26:49Z dkocher $
 */
public class MainController extends BundleController implements NSApplication.Delegate {
    private static Logger log = Logger.getLogger(MainController.class);

    /**
     * Apple event constants<br>
     * **********************************************************************************************<br>
     * <i>native declaration : /Developer/SDKs/MacOSX10.5.sdk/usr/include/AvailabilityMacros.h:117</i>
     */
    public static final int kInternetEventClass = 1196773964;
    /**
     * Apple event constants<br>
     * **********************************************************************************************<br>
     * <i>native declaration : /Developer/SDKs/MacOSX10.5.sdk/usr/include/AvailabilityMacros.h:118</i>
     */
    public static final int kAEGetURL = 1196773964;
    /**
     * Apple event constants<br>
     * **********************************************************************************************<br>
     * <i>native declaration : /Developer/SDKs/MacOSX10.5.sdk/usr/include/AvailabilityMacros.h:119</i>
     */
    public static final int kAEFetchURL = 1179996748;

    /// 0x2d2d2d2d
    public static final int keyAEResult = 757935405;

    public MainController() {
        this.loadBundle();
    }

    @Override
    public void awakeFromNib() {
        NSAppleEventManager.sharedAppleEventManager().setEventHandler_andSelector_forEventClass_andEventID(
                this.id(), Foundation.selector("handleGetURLEvent:withReplyEvent:"), kInternetEventClass, kAEGetURL);

        super.awakeFromNib();
    }

    private PathKindDetector detector = new DefaultPathKindDetector();

    /**
     * Extract the URL from the Apple event and handle it here.
     */
    public void handleGetURLEvent_withReplyEvent(NSAppleEventDescriptor event, NSAppleEventDescriptor reply) {
        log.debug("Received URL from Apple Event:" + event);
        final NSAppleEventDescriptor param = event.paramDescriptorForKeyword(keyAEResult);
        if(null == param) {
            log.error("No URL parameter");
            return;
        }
        final String url = param.stringValue();
        if(StringUtils.isEmpty(url)) {
            log.error("URL parameter is empty");
            return;
        }
        if("x-cyberduck-action:update".equals(url)) {
            this.updateMenuClicked(null);
        }
        else {
            final Host h = Host.parse(url);
            if(Path.FILE_TYPE == detector.detect(h.getDefaultPath())) {
                final Session s = SessionFactory.createSession(h);
                final Path p = PathFactory.createPath(s, h.getDefaultPath(), Path.FILE_TYPE);
                TransferController.instance().startTransfer(new DownloadTransfer(p));
            }
            else {
                for(BrowserController controller : MainController.getBrowsers()) {
                    if(controller.isMounted()) {
                        if(controller.getSession().getHost().toURL().equals(h.toURL())) {
                            // Handle browser window already connected to the same host. #4215
                            controller.window().makeKeyAndOrderFront(null);
                            return;
                        }
                    }
                }
                BrowserController doc = newDocument();
                doc.mount(h);
            }
        }
    }

    private Updater updater;

    @Action
    public void updateMenuClicked(ID sender) {
        if(null == updater) {
            updater = Updater.create();
        }
        updater.checkForUpdates(null);
    }

    @Outlet
    private NSMenu applicationMenu;

    public void setApplicationMenu(NSMenu menu) {
        this.applicationMenu = menu;
        this.updateLicenseMenu();
        this.updateUpdateMenu();
    }

    /**
     * Set name of key in menu item
     */
    private void updateLicenseMenu() {
        License key = LicenseFactory.find();
        if(null == Updater.getFeed() && key.isReceipt()) {
            this.applicationMenu.removeItemAtIndex(new NSInteger(5));
            this.applicationMenu.removeItemAtIndex(new NSInteger(4));
        }
        else {
            NSDictionary KEY_FONT_ATTRIBUTES = NSDictionary.dictionaryWithObjectsForKeys(
                    NSArray.arrayWithObjects(NSFont.userFontOfSize(NSFont.smallSystemFontSize()), NSColor.darkGrayColor()),
                    NSArray.arrayWithObjects(NSAttributedString.FontAttributeName, NSAttributedString.ForegroundColorAttributeName)
            );
            this.applicationMenu.itemAtIndex(new NSInteger(5)).setAttributedTitle(
                    NSAttributedString.attributedStringWithAttributes(key.toString(), KEY_FONT_ATTRIBUTES)
            );
        }
    }

    /**
     * Remove software update menu item if no update feed available
     */
    private void updateUpdateMenu() {
        if(null == Updater.getFeed()) {
            this.applicationMenu.removeItemAtIndex(new NSInteger(1));
        }
    }

    @Outlet
    private NSMenu encodingMenu;

    public void setEncodingMenu(NSMenu encodingMenu) {
        this.encodingMenu = encodingMenu;
        for(String charset : availableCharsets()) {
            this.encodingMenu.addItemWithTitle_action_keyEquivalent(charset, Foundation.selector("encodingMenuClicked:"), StringUtils.EMPTY);
        }
    }

    @Outlet
    private NSMenu columnMenu;

    public void setColumnMenu(NSMenu columnMenu) {
        this.columnMenu = columnMenu;
        Map<String, String> columns = new HashMap<String, String>();
        columns.put("browser.columnKind", Locale.localizedString("Kind"));
        columns.put("browser.columnExtension", Locale.localizedString("Extension"));
        columns.put("browser.columnSize", Locale.localizedString("Size"));
        columns.put("browser.columnModification", Locale.localizedString("Modified"));
        columns.put("browser.columnOwner", Locale.localizedString("Owner"));
        columns.put("browser.columnGroup", Locale.localizedString("Group"));
        columns.put("browser.columnPermissions", Locale.localizedString("Permissions"));
        for(Map.Entry<String, String> entry : columns.entrySet()) {
            NSMenuItem item = this.columnMenu.addItemWithTitle_action_keyEquivalent(entry.getValue(),
                    Foundation.selector("columnMenuClicked:"), StringUtils.EMPTY);
            final String identifier = entry.getKey();
            item.setState(Preferences.instance().getBoolean(identifier) ? NSCell.NSOnState : NSCell.NSOffState);
            item.setRepresentedObject(identifier);
        }
    }

    @Action
    public void columnMenuClicked(final NSMenuItem sender) {
        final String identifier = sender.representedObject();
        final boolean enabled = !Preferences.instance().getBoolean(identifier);
        sender.setState(enabled ? NSCell.NSOnState : NSCell.NSOffState);
        Preferences.instance().setProperty(identifier, enabled);
        BrowserController.updateBrowserTableColumns();
    }

    @Outlet
    private NSMenu editMenu;
    private EditMenuDelegate editMenuDelegate;

    public void setEditMenu(NSMenu editMenu) {
        this.editMenu = editMenu;
        this.editMenuDelegate = new EditMenuDelegate() {
            @Override
            protected Path getEditable() {
                final List<BrowserController> b = MainController.getBrowsers();
                for(BrowserController controller : b) {
                    if(controller.window().isKeyWindow()) {
                        final Path selected = controller.getSelectedPath();
                        if(null == selected) {
                            return null;
                        }
                        if(controller.isEditable(selected)) {
                            return selected;
                        }
                        return null;
                    }
                }
                return null;
            }

            @Override
            protected ID getTarget() {
                return MainController.getBrowser().id();
            }
        };
        this.editMenu.setDelegate(editMenuDelegate.id());
    }

    @Outlet
    private NSMenu urlMenu;
    private URLMenuDelegate urlMenuDelegate;

    public void setUrlMenu(NSMenu urlMenu) {
        this.urlMenu = urlMenu;
        this.urlMenuDelegate = new CopyURLMenuDelegate() {
            @Override
            protected List<Path> getSelected() {
                final List<BrowserController> b = MainController.getBrowsers();
                for(BrowserController controller : b) {
                    if(controller.window().isKeyWindow()) {
                        List<Path> selected = controller.getSelectedPaths();
                        if(selected.isEmpty()) {
                            if(controller.isMounted()) {
                                return Collections.singletonList(controller.workdir());
                            }
                        }
                        return selected;
                    }
                }
                return Collections.emptyList();
            }
        };
        this.urlMenu.setDelegate(urlMenuDelegate.id());
    }

    @Outlet
    private NSMenu openUrlMenu;
    private URLMenuDelegate openUrlMenuDelegate;

    public void setOpenUrlMenu(NSMenu openUrlMenu) {
        this.openUrlMenu = openUrlMenu;
        this.openUrlMenuDelegate = new OpenURLMenuDelegate() {
            @Override
            protected List<Path> getSelected() {
                final List<BrowserController> b = MainController.getBrowsers();
                for(BrowserController controller : b) {
                    if(controller.window().isKeyWindow()) {
                        List<Path> selected = controller.getSelectedPaths();
                        if(selected.isEmpty()) {
                            if(controller.isMounted()) {
                                return Collections.singletonList(controller.workdir());
                            }
                        }
                        return selected;
                    }
                }
                return Collections.emptyList();
            }
        };
        this.openUrlMenu.setDelegate(openUrlMenuDelegate.id());
    }

    @Outlet
    private NSMenu archiveMenu;
    private ArchiveMenuDelegate archiveMenuDelegate;

    public void setArchiveMenu(NSMenu archiveMenu) {
        this.archiveMenu = archiveMenu;
        this.archiveMenuDelegate = new ArchiveMenuDelegate() {
            @Override
            protected ID getTarget() {
                return MainController.getBrowser().id();
            }
        };
        this.archiveMenu.setDelegate(archiveMenuDelegate.id());
    }

    @Outlet
    private NSMenu bookmarkMenu;
    private BookmarkMenuDelegate bookmarkMenuDelegate;

    public void setBookmarkMenu(NSMenu bookmarkMenu) {
        this.bookmarkMenu = bookmarkMenu;
        this.bookmarkMenuDelegate = new BookmarkMenuDelegate() {
            @Override
            protected ID getTarget() {
                return MainController.getBrowser().id();
            }
        };
        this.bookmarkMenu.setDelegate(bookmarkMenuDelegate.id());
    }

    @Outlet
    private NSMenu historyMenu;
    private HistoryMenuDelegate historyMenuDelegate;

    public void setHistoryMenu(NSMenu historyMenu) {
        this.historyMenu = historyMenu;
        this.historyMenuDelegate = new HistoryMenuDelegate() {
            @Override
            protected ID getTarget() {
                return MainController.getBrowser().id();
            }
        };
        this.historyMenu.setDelegate(historyMenuDelegate.id());
    }

    @Outlet
    private NSMenu rendezvousMenu;
    private RendezvousMenuDelegate rendezvousMenuDelegate;

    public void setRendezvousMenu(NSMenu rendezvousMenu) {
        this.rendezvousMenu = rendezvousMenu;
        this.rendezvousMenuDelegate = new RendezvousMenuDelegate() {
            @Override
            protected ID getTarget() {
                return MainController.getBrowser().id();
            }
        };
        this.rendezvousMenu.setDelegate(rendezvousMenuDelegate.id());
    }

    @Action
    public void historyMenuClicked(NSMenuItem sender) {
        HistoryCollection.defaultCollection().open();
    }

    @Action
    public void bugreportMenuClicked(final ID sender) {
        openUrl(Preferences.instance().getProperty("website.bug"));
    }

    @Action
    public void helpMenuClicked(final ID sender) {
        openUrl(Preferences.instance().getProperty("website.help"));
    }

    @Action
    public void licenseMenuClicked(final ID sender) {
        ApplicationLauncherFactory.get().open(
                LocalFactory.createLocal(NSBundle.mainBundle().pathForResource_ofType("License", "txt")));
    }

    @Action
    public void acknowledgmentsMenuClicked(final ID sender) {
        ApplicationLauncherFactory.get().open(
                LocalFactory.createLocal(NSBundle.mainBundle().pathForResource_ofType("Acknowledgments", "rtf")));
    }

    @Action
    public void websiteMenuClicked(final ID sender) {
        openUrl(Preferences.instance().getProperty("website.home"));
    }

    @Action
    public void forumMenuClicked(final ID sender) {
        openUrl(Preferences.instance().getProperty("website.forum"));
    }

    @Action
    public void donateMenuClicked(final ID sender) {
        openUrl(Preferences.instance().getProperty("website.donate"));
    }

    @Action
    public void aboutMenuClicked(final ID sender) {
        NSDictionary dict = NSDictionary.dictionaryWithObjectsForKeys(
                NSArray.arrayWithObjects(
                        NSBundle.mainBundle().objectForInfoDictionaryKey("CFBundleShortVersionString").toString(),
                        NSBundle.mainBundle().objectForInfoDictionaryKey("CFBundleVersion").toString()),
                NSArray.arrayWithObjects(
                        "ApplicationVersion",
                        "Version")
        );
        NSApplication.sharedApplication().orderFrontStandardAboutPanelWithOptions(dict);
    }

    @Action
    public void feedbackMenuClicked(final ID sender) {
        openUrl(Preferences.instance().getProperty("mail.feedback")
                + "?subject=" + Preferences.instance().getProperty("application.name") + "-" + Preferences.instance().getProperty("application.version"));
    }

    @Action
    public void preferencesMenuClicked(final ID sender) {
        PreferencesController controller = PreferencesController.instance();
        controller.window().makeKeyAndOrderFront(null);
    }

    @Action
    public void newDownloadMenuClicked(final ID sender) {
        this.showTransferQueueClicked(sender);
        SheetController c = new DownloadController(TransferController.instance());
        c.beginSheet();
    }

    @Action
    public void newBrowserMenuClicked(final ID sender) {
        this.openDefaultBookmark(MainController.newDocument(true));
    }

    @Action
    public void showTransferQueueClicked(final ID sender) {
        TransferController c = TransferController.instance();
        c.window().makeKeyAndOrderFront(null);
    }

    @Action
    public void showActivityWindowClicked(final ID sender) {
        ActivityController c = ActivityController.instance();
        if(c.isVisible()) {
            c.window().close();
        }
        else {
            c.window().orderFront(null);
        }
    }

    @Override
    public boolean application_openFile(NSApplication app, String filename) {
        log.debug("applicationOpenFile:" + filename);
        final Local f = LocalFactory.createLocal(filename);
        if(f.exists()) {
            if("duck".equals(f.getExtension())) {
                final Host bookmark = HostReaderFactory.get().read(f);
                if(null == bookmark) {
                    return false;
                }
                MainController.newDocument().mount(bookmark);
                return true;
            }
            else if("cyberducklicense".equals(f.getExtension())) {
                final License l = LicenseFactory.create(f);
                if(l instanceof Donation) {
                    if(l.verify()) {
                        final NSAlert alert = NSAlert.alert(
                                l.toString(),
                                Locale.localizedString("Thanks for your support! Your contribution helps to further advance development to make Cyberduck even better.", "License")
                                        + "\n\n"
                                        + Locale.localizedString("Your donation key has been copied to the Application Support folder.", "License"),
                                Locale.localizedString("Continue", "License"), //default
                                null, //other
                                null);
                        alert.setAlertStyle(NSAlert.NSInformationalAlertStyle);
                        if(this.alert(alert) == SheetCallback.DEFAULT_OPTION) {
                            f.copy(LocalFactory.createLocal(Preferences.instance().getProperty("application.support.path"),
                                    f.getName()));
                            for(BrowserController c : MainController.getBrowsers()) {
                                c.removeDonateWindowTitle();
                            }
                            this.updateLicenseMenu();
                        }
                    }
                    else {
                        final NSAlert alert = NSAlert.alert(
                                Locale.localizedString("Not a valid donation key", "License"),
                                Locale.localizedString("This donation key does not appear to be valid.", "License"),
                                Locale.localizedString("Continue", "License"), //default
                                null, //other
                                null);
                        alert.setAlertStyle(NSAlert.NSWarningAlertStyle);
                        alert.setShowsHelp(true);
                        alert.setDelegate(new ProxyController() {
                            public boolean alertShowHelp(NSAlert alert) {
                                StringBuilder site = new StringBuilder(Preferences.instance().getProperty("website.help"));
                                site.append("/").append("faq");
                                openUrl(site.toString());
                                return true;
                            }

                        }.id());
                        this.alert(alert);
                    }
                }
                return true;
            }
            else if("cyberduckprofile".equals(f.getExtension())) {
                final Protocol profile = ProfileReaderFactory.get().read(f);
                if(null == profile) {
                    return false;
                }
                if(profile.isEnabled()) {
                    profile.register();
                    final Host host = new Host(profile, profile.getDefaultHostname(), profile.getDefaultPort());
                    MainController.newDocument().addBookmark(host);
                    // Register in application support
                    final Local profiles = LocalFactory.createLocal(Preferences.instance().getProperty("application.support.path"), "Profiles");
                    profiles.mkdir();
                    f.copy(LocalFactory.createLocal(profiles, f.getName()));
                }
            }
            else {
                // Upload file
                this.background(new AbstractBackgroundAction<Void>() {
                    @Override
                    public void run() {
                        // Wait until bookmarks are loaded
                        try {
                            loader.await();
                        }
                        catch(InterruptedException e) {
                            log.error(e.getMessage());
                        }
                    }

                    @Override
                    public void cleanup() {
                        upload(f);
                    }

                    @Override
                    public String getActivity() {
                        return "Open File";
                    }
                });
                return true;
            }
        }
        return false;
    }

    private boolean upload(Local f) {
        return this.upload(Collections.singletonList(f));
    }

    private boolean upload(final List<Local> files) {
        // Selected bookmark
        Host open = null;
        String workdir = String.valueOf(Path.DELIMITER);
        for(BrowserController controller : MainController.getBrowsers()) {
            if(controller.isMounted()) {
                open = controller.getSession().getHost();
                workdir = controller.workdir().getAbsolute();
                if(1 == MainController.getBrowsers().size()) {
                    // If only one browser window upload to current working directory with no bookmark selection
                    this.upload(open, files, workdir);
                    return true;
                }
                break;
            }
        }
        if(BookmarkCollection.defaultCollection().isEmpty()) {
            log.warn("No bookmark for upload");
            return false;
        }
        final NSPopUpButton bookmarksPopup = NSPopUpButton.buttonWithFrame(new NSRect(0, 26));
        bookmarksPopup.setToolTip(Locale.localizedString("Bookmarks"));
        for(Host b : BookmarkCollection.defaultCollection()) {
            String title = b.getNickname();
            int i = 1;
            while(bookmarksPopup.itemWithTitle(title) != null) {
                title = b.getNickname() + "-" + i;
                i++;
            }
            bookmarksPopup.addItemWithTitle(title);
            bookmarksPopup.lastItem().setImage(IconCache.iconNamed(b.getProtocol().icon(), 16));
            bookmarksPopup.lastItem().setRepresentedObject(b.getUuid());
            if(b.equals(open)) {
                bookmarksPopup.selectItemAtIndex(bookmarksPopup.indexOfItem(bookmarksPopup.lastItem()));
            }
        }
        if(null == open) {
            int i = 0;
            for(Host bookmark : BookmarkCollection.defaultCollection()) {
                boolean found = false;
                // Pick the bookmark with the same download location
                for(Local file : files) {
                    if(file.isChild(bookmark.getDownloadFolder())) {
                        bookmarksPopup.selectItemAtIndex(new NSInteger(i));
                        found = true;
                        break;
                    }
                }
                if(found) {
                    break;
                }
                i++;
            }
        }
        if(-1 == bookmarksPopup.indexOfSelectedItem().intValue()) {
            // No bookmark for current browser found
            bookmarksPopup.selectItemAtIndex(new NSInteger(0));
        }
        final TransferController t = TransferController.instance();
        final Host mount = open;
        final String destination = workdir;
        AlertController alert = new AlertController(t, NSAlert.alert("Select Bookmark",
                MessageFormat.format("Upload {0} to the selected bookmark.",
                        files.size() == 1 ? files.iterator().next().getName()
                                : MessageFormat.format(Locale.localizedString("{0} Files"), String.valueOf(files.size()))),
                Locale.localizedString("Upload"),
                Locale.localizedString("Cancel"),
                null)) {
            @Override
            public void callback(int returncode) {
                if(DEFAULT_OPTION == returncode) {
                    final String selected = bookmarksPopup.selectedItem().representedObject();
                    for(Host bookmark : BookmarkCollection.defaultCollection()) {
                        // Determine selected bookmark
                        if(bookmark.getUuid().equals(selected)) {
                            String parent = destination;
                            if(bookmark.equals(mount)) {
                                // Use current working directory of browser for destination
                            }
                            else {
                                // No mounted browser
                                if(StringUtils.isNotBlank(bookmark.getDefaultPath())) {
                                    parent = bookmark.getDefaultPath();
                                }
                            }
                            upload(bookmark, files, parent);
                            break;
                        }
                    }
                }
            }

            @Override
            protected boolean validateInput() {
                return StringUtils.isNotEmpty(bookmarksPopup.selectedItem().representedObject());
            }
        };
        alert.setAccessoryView(bookmarksPopup);
        alert.beginSheet();
        return true;
    }

    private void upload(Host bookmark, List<Local> files, String destination) {
        final Session session = SessionFactory.createSession(bookmark);
        List<Path> roots = new ArrayList<Path>();
        for(Local file : files) {
            roots.add(PathFactory.createPath(session, destination, file));
        }
        final TransferController t = TransferController.instance();
        t.startTransfer(new UploadTransfer(roots));
    }

    /**
     * Sent directly by theApplication to the delegate. The method should attempt to open the file filename,
     * returning true if the file is successfully opened, and false otherwise. By design, a
     * file opened through this method is assumed to be temporary its the application's
     * responsibility to remove the file at the appropriate time.
     */
    @Override
    public boolean application_openTempFile(NSApplication app, String filename) {
        log.debug("applicationOpenTempFile:" + filename);
        return this.application_openFile(app, filename);
    }

    /**
     * Invoked immediately before opening an untitled file. Return false to prevent
     * the application from opening an untitled file; return true otherwise.
     * Note that applicationOpenUntitledFile is invoked if this method returns true.
     */
    @Override
    public boolean applicationShouldOpenUntitledFile(NSApplication sender) {
        log.debug("applicationShouldOpenUntitledFile");
        return Preferences.instance().getBoolean("browser.openUntitled");
    }

    /**
     * @return true if the file was successfully opened, false otherwise.
     */
    @Override
    public boolean applicationOpenUntitledFile(NSApplication app) {
        log.debug("applicationOpenUntitledFile");
        return false;
    }

    /**
     * Mounts the default bookmark if any
     */
    private void openDefaultBookmark(BrowserController controller) {
        String defaultBookmark = Preferences.instance().getProperty("browser.defaultBookmark");
        if(null == defaultBookmark) {
            log.info("No default bookmark configured");
            return; //No default bookmark given
        }
        Host bookmark = BookmarkCollection.defaultCollection().lookup(defaultBookmark);
        if(null == bookmark) {
            log.info("Default bookmark no more available");
            return;
        }
        for(BrowserController browser : getBrowsers()) {
            if(browser.hasSession()) {
                if(browser.getSession().getHost().equals(bookmark)) {
                    log.debug("Default bookmark already mounted");
                    return;
                }
            }
        }
        log.debug("Mounting default bookmark " + bookmark);
        controller.mount(bookmark);
    }

    /**
     * These events are sent whenever the Finder reactivates an already running application
     * because someone double-clicked it again or used the dock to activate it. By default
     * the Application Kit will handle this event by checking whether there are any visible
     * NSWindows (not NSPanels), and, if there are none, it goes through the standard untitled
     * document creation (the same as it does if theApplication is launched without any document
     * to open). For most document-based applications, an untitled document will be created.
     * The application delegate will also get a chance to respond to the normal untitled document
     * delegations. If you implement this method in your application delegate, it will be called
     * before any of the default behavior happens. If you return true, then NSApplication will
     * go on to do its normal thing. If you return false, then NSApplication will do nothing.
     * So, you can either implement this method, do nothing, and return false if you do not
     * want anything to happen at all (not recommended), or you can implement this method,
     * handle the event yourself in some custom way, and return false.
     */
    @Override
    public boolean applicationShouldHandleReopen_hasVisibleWindows(NSApplication app, boolean visibleWindowsFound) {
        log.debug("applicationShouldHandleReopen");
        // While an application is open, the Dock icon has a symbol below it.
        // When a user clicks an open application’s icon in the Dock, the application
        // becomes active and all open unminimized windows are brought to the front;
        // minimized document windows remain in the Dock. If there are no unminimized
        // windows when the user clicks the Dock icon, the last minimized window should
        // be expanded and made active. If no documents are open, the application should
        // open a new window. (If your application is not document-based, display the
        // application’s main window.)
        if(MainController.getBrowsers().isEmpty() && !TransferController.instance().isVisible()) {
            this.openDefaultBookmark(MainController.newDocument());
        }
        NSWindow miniaturized = null;
        for(BrowserController controller : MainController.getBrowsers()) {
            if(!controller.window().isMiniaturized()) {
                return false;
            }
            if(null == miniaturized) {
                miniaturized = controller.window();
            }
        }
        if(null == miniaturized) {
            return false;
        }
        miniaturized.deminiaturize(null);
        return false;
    }

    // User bookmarks and thirdparty applications
    private final CountDownLatch loader = new CountDownLatch(2);

    /**
     * Sent by the default notification center after the application has been launched and initialized but
     * before it has received its first event. aNotification is always an
     * ApplicationDidFinishLaunchingNotification. You can retrieve the NSApplication
     * object in question by sending object to aNotification. The delegate can implement
     * this method to perform further initialization. If the user started up the application
     * by double-clicking a file, the delegate receives the applicationOpenFile message before receiving
     * applicationDidFinishLaunching. (applicationWillFinishLaunching is sent before applicationOpenFile.)
     */
    @Override
    public void applicationDidFinishLaunching(NSNotification notification) {
        if(log.isInfoEnabled()) {
            log.info(String.format("Running version %s", NSBundle.mainBundle().objectForInfoDictionaryKey("CFBundleVersion").toString()));
            log.info(String.format("Running Java %s on %s", System.getProperty("java.version"), System.getProperty("os.arch")));
            log.info(String.format("Available localizations:%s", NSBundle.mainBundle().localizations()));
            log.info(String.format("Current locale:%s", java.util.Locale.getDefault()));
            log.info(String.format("Native library path:%s", System.getProperty("java.library.path")));
        }
        if(Preferences.instance().getBoolean("browser.openUntitled")) {
            MainController.newDocument();
        }
        if(Preferences.instance().getBoolean("queue.openByDefault")) {
            this.showTransferQueueClicked(null);
        }
        if(Preferences.instance().getBoolean("browser.serialize")) {
            this.background(new AbstractBackgroundAction<Void>() {
                @Override
                public void run() {
                    sessions.load();
                }

                @Override
                public void cleanup() {
                    for(Host host : sessions) {
                        MainController.newDocument().mount(host);
                    }
                    sessions.clear();
                }
            });
        }

        this.background(new AbstractBackgroundAction<Void>() {
            @Override
            public void run() {
                final BookmarkCollection c = BookmarkCollection.defaultCollection();
                c.load();
                loader.countDown();
            }

            @Override
            public void cleanup() {
                if(Preferences.instance().getBoolean("browser.openUntitled")) {
                    if(MainController.getBrowsers().isEmpty()) {
                        openDefaultBookmark(MainController.newDocument());
                    }
                }
                // Set delegate for NSService
                NSApplication.sharedApplication().setServicesProvider(MainController.this.id());
            }

            @Override
            public String getActivity() {
                return "Loading Bookmarks";
            }
        });
        this.background(new AbstractBackgroundAction<Void>() {
            @Override
            public void run() {
                HistoryCollection.defaultCollection().load();
            }

            @Override
            public String getActivity() {
                return "Loading History";
            }
        });
        this.background(new AbstractBackgroundAction<Void>() {
            @Override
            public void run() {
                TransferCollection.defaultCollection().load();
            }

            @Override
            public String getActivity() {
                return "Loading Transfers";
            }
        });
        this.background(new AbstractBackgroundAction<Void>() {
            @Override
            public void run() {
                // Make sure we register to Growl first
                Growl.instance().setup();
            }

            @Override
            public String getActivity() {
                return "Registering Growl";
            }
        });
        if(Preferences.instance().getBoolean("rendezvous.enable")) {
            RendezvousFactory.instance().addListener(new RendezvousListener() {
                @Override
                public void serviceResolved(final String identifier, final Host host) {
                    if(Preferences.instance().getBoolean("rendezvous.loopback.supress")) {
                        try {
                            if(InetAddress.getByName(host.getHostname()).equals(InetAddress.getLocalHost())) {
                                log.info("Supressed Rendezvous notification for " + host);
                                return;
                            }
                        }
                        catch(UnknownHostException e) {
                            //Ignore
                        }
                    }
                    invoke(new DefaultMainAction() {
                        @Override
                        public void run() {
                            Growl.instance().notifyWithImage("Bonjour", RendezvousFactory.instance().getDisplayedName(identifier), "rendezvous");
                        }
                    });
                }

                @Override
                public void serviceLost(String servicename) {
                    //
                }
            });
        }
        if(Preferences.instance().getBoolean("defaulthandler.reminder")
                && Preferences.instance().getInteger("uses") > 0) {
            if(!SchemeHandlerFactory.get().isDefaultHandler(
                    Arrays.asList(Protocol.FTP.getScheme(), Protocol.FTP_TLS.getScheme(), Protocol.SFTP.getScheme()),
                    new Application(NSBundle.mainBundle().infoDictionary().objectForKey("CFBundleIdentifier").toString(), null))) {
                final NSAlert alert = NSAlert.alert(
                        Locale.localizedString("Set Cyberduck as default application for FTP and SFTP locations?", "Configuration"),
                        Locale.localizedString("As the default application, Cyberduck will open when you click on FTP or SFTP links in other applications, such as your web browser. You can change this setting in the Preferences later.", "Configuration"),
                        Locale.localizedString("Change", "Configuration"), //default
                        null, //other
                        Locale.localizedString("Cancel", "Configuration"));
                alert.setAlertStyle(NSAlert.NSInformationalAlertStyle);
                alert.setShowsSuppressionButton(true);
                alert.suppressionButton().setTitle(Locale.localizedString("Don't ask again", "Configuration"));
                int choice = alert.runModal(); //alternate
                if(alert.suppressionButton().state() == NSCell.NSOnState) {
                    // Never show again.
                    Preferences.instance().setProperty("defaulthandler.reminder", false);
                }
                if(choice == SheetCallback.DEFAULT_OPTION) {
                    SchemeHandlerFactory.get().setDefaultHandler(
                            Arrays.asList(Protocol.FTP.getScheme(), Protocol.FTP_TLS.getScheme(), Protocol.SFTP.getScheme()),
                            new Application(NSBundle.mainBundle().infoDictionary().objectForKey("CFBundleIdentifier").toString(), null)
                    );
                }
            }
        }
        // NSWorkspace notifications are posted to a notification center provided by
        // the NSWorkspace object, instead of going through the application’s default
        // notification center as most notifications do. To receive NSWorkspace notifications,
        // your application must register an observer with the NSWorkspace notification center.
        NSWorkspace.sharedWorkspace().notificationCenter().addObserver(this.id(),
                Foundation.selector("workspaceWillPowerOff:"),
                NSWorkspace.WorkspaceWillPowerOffNotification,
                null);
        NSWorkspace.sharedWorkspace().notificationCenter().addObserver(this.id(),
                Foundation.selector("workspaceWillLogout:"),
                NSWorkspace.WorkspaceSessionDidResignActiveNotification,
                null);
        NSWorkspace.sharedWorkspace().notificationCenter().addObserver(this.id(),
                Foundation.selector("workspaceWillSleep:"),
                NSWorkspace.WorkspaceWillSleepNotification,
                null);
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("applicationWillRestartAfterUpdate:"),
                "SUUpdaterWillRestartNotificationName",
                null);
        if(Preferences.instance().getBoolean("rendezvous.enable")) {
            this.background(new AbstractBackgroundAction<Void>() {
                @Override
                public void run() {
                    RendezvousFactory.instance().init();
                }
            });
        }
        // Import thirdparty bookmarks.
        this.background(new AbstractBackgroundAction<Void>() {
            private List<ThirdpartyBookmarkCollection> bookmarks = Collections.emptyList();

            @Override
            public void run() {
                bookmarks = this.getThirdpartyBookmarks();
                for(ThirdpartyBookmarkCollection c : bookmarks) {
                    if(!Preferences.instance().getBoolean(c.getConfiguration())) {
                        if(!c.isInstalled()) {
                            log.info("No application installed for " + c.getBundleIdentifier());
                            continue;
                        }
                        c.load();
                        if(c.isEmpty()) {
                            // Flag as imported
                            Preferences.instance().setProperty(c.getConfiguration(), true);
                        }
                    }
                }
            }

            @Override
            public void cleanup() {
                for(ThirdpartyBookmarkCollection c : bookmarks) {
                    if(c.isEmpty()) {
                        continue;
                    }
                    final NSAlert alert = NSAlert.alert(
                            MessageFormat.format(Locale.localizedString("Import {0} Bookmarks", "Configuration"), c.getName()),
                            MessageFormat.format(Locale.localizedString("{0} bookmarks found. Do you want to add these to your bookmarks?", "Configuration"), c.size()),
                            Locale.localizedString("Import", "Configuration"), //default
                            null, //other
                            Locale.localizedString("Cancel", "Configuration"));
                    alert.setShowsSuppressionButton(true);
                    alert.suppressionButton().setTitle(Locale.localizedString("Don't ask again", "Configuration"));
                    alert.setAlertStyle(NSAlert.NSInformationalAlertStyle);
                    int choice = alert.runModal(); //alternate
                    if(alert.suppressionButton().state() == NSCell.NSOnState) {
                        // Never show again.
                        Preferences.instance().setProperty(c.getConfiguration(), true);
                    }
                    if(choice == SheetCallback.DEFAULT_OPTION) {
                        BookmarkCollection.defaultCollection().addAll(c);
                        // Flag as imported
                        Preferences.instance().setProperty(c.getConfiguration(), true);
                    }
                }
                loader.countDown();
            }

            @Override
            public String getActivity() {
                return "Loading thirdparty bookmarks";
            }

            private List<ThirdpartyBookmarkCollection> getThirdpartyBookmarks() {
                return Arrays.asList(new TransmitBookmarkCollection(), new FilezillaBookmarkCollection(), new FetchBookmarkCollection(),
                        new FlowBookmarkCollection(), new InterarchyBookmarkCollection(), new CrossFtpBookmarkCollection(), new FireFtpBookmarkCollection());
            }
        });
        this.background(new AbstractBackgroundAction<Void>() {
            @Override
            public void run() {
                // Wait until bookmarks are loaded
                try {
                    loader.await();
                }
                catch(InterruptedException e) {
                    log.error(e.getMessage());
                }
                final BookmarkCollection c = BookmarkCollection.defaultCollection();
                if(c.isEmpty()) {
                    final FolderBookmarkCollection defaults = new FolderBookmarkCollection(LocalFactory.createLocal(
                            Preferences.instance().getProperty("application.bookmarks.path")
                    )) {
                        private static final long serialVersionUID = -6110285052565190698L;

                        @Override
                        protected void rename(Local next, Host bookmark) {
                            // Bookmarks in application bundle should not attempt to be renamed to UUID
                        }
                    };
                    defaults.load();
                    for(Host bookmark : defaults) {
                        if(log.isDebugEnabled()) {
                            log.debug("Adding default bookmark:" + bookmark);
                        }
                        c.add(bookmark);
                    }
                }
            }

            @Override
            public String getActivity() {
                return "Loading Default Bookmarks";
            }
        });
    }

    /**
     * NSService implementation
     */
    public void serviceUploadFileUrl_(final NSPasteboard pboard, String userData) {
        log.debug("serviceUploadFileUrl_:" + userData);
        if(pboard.availableTypeFromArray(NSArray.arrayWithObject(NSPasteboard.FilenamesPboardType)) != null) {
            NSObject o = pboard.propertyListForType(NSPasteboard.FilenamesPboardType);
            if(o != null) {
                final NSArray elements = Rococoa.cast(o, NSArray.class);
                List<Local> files = new ArrayList<Local>();
                for(int i = 0; i < elements.count().intValue(); i++) {
                    files.add(LocalFactory.createLocal(elements.objectAtIndex(new NSUInteger(i)).toString()));
                }
                this.upload(files);
            }
        }
    }

    /**
     * Saved browsers
     */
    private HistoryCollection sessions = new HistoryCollection(
            LocalFactory.createLocal(Preferences.instance().getProperty("application.support.path"), "Sessions"));

    /**
     * Display donation reminder dialog
     */
    private boolean displayDonationPrompt = true;
    private WindowController donationController;


    /**
     * Invoked from within the terminate method immediately before the
     * application terminates. sender is the NSApplication to be terminated.
     * If this method returns false, the application is not terminated,
     * and control returns to the main event loop.
     *
     * @param app Application instance
     * @return Return true to allow the application to terminate.
     */
    @Override
    public NSUInteger applicationShouldTerminate(final NSApplication app) {
        log.debug("applicationShouldTerminate");
        // Determine if there are any running transfers
        NSUInteger result = TransferController.applicationShouldTerminate(app);
        if(!result.equals(NSApplication.NSTerminateNow)) {
            return result;
        }
        // Determine if there are any open connections
        for(BrowserController controller : MainController.getBrowsers()) {
            if(Preferences.instance().getBoolean("browser.serialize")) {
                if(controller.isMounted()) {
                    // The workspace should be saved. Serialize all open browser sessions
                    final Host serialized = new Host(controller.getSession().getHost().getAsDictionary());
                    serialized.setWorkdir(controller.workdir().getAbsolute());
                    sessions.add(serialized);
                }
            }
            if(controller.isConnected()) {
                if(Preferences.instance().getBoolean("browser.confirmDisconnect")) {
                    final NSAlert alert = NSAlert.alert(Locale.localizedString("Quit"),
                            Locale.localizedString("You are connected to at least one remote site. Do you want to review open browsers?"),
                            Locale.localizedString("Quit Anyway"), //default
                            Locale.localizedString("Cancel"), //other
                            Locale.localizedString("Review…"));
                    alert.setAlertStyle(NSAlert.NSWarningAlertStyle);
                    alert.setShowsSuppressionButton(true);
                    alert.suppressionButton().setTitle(Locale.localizedString("Don't ask again", "Configuration"));
                    int choice = alert.runModal(); //alternate
                    if(alert.suppressionButton().state() == NSCell.NSOnState) {
                        // Never show again.
                        Preferences.instance().setProperty("browser.confirmDisconnect", false);
                    }
                    if(choice == SheetCallback.CANCEL_OPTION) {
                        // Cancel. Quit has been interrupted. Delete any saved sessions so far.
                        sessions.clear();
                        return NSApplication.NSTerminateCancel;
                    }
                    if(choice == SheetCallback.ALTERNATE_OPTION) {
                        // Review if at least one window reqested to terminate later, we shall wait.
                        // This will iterate over all mounted browsers.
                        result = BrowserController.applicationShouldTerminate(app);
                        if(NSApplication.NSTerminateNow.equals(result)) {
                            return this.applicationShouldTerminateAfterDonationPrompt(app);
                        }
                        return result;
                    }
                    if(choice == SheetCallback.DEFAULT_OPTION) {
                        // Quit immediatly
                        return this.applicationShouldTerminateAfterDonationPrompt(app);
                    }
                }
                else {
                    controller.unmount();
                }
            }
        }
        return this.applicationShouldTerminateAfterDonationPrompt(app);
    }

    public NSUInteger applicationShouldTerminateAfterDonationPrompt(final NSApplication app) {
        log.debug("applicationShouldTerminateAfterDonationPrompt");
        if(!displayDonationPrompt) {
            // Already displayed
            return NSApplication.NSTerminateNow;
        }
        final License l = LicenseFactory.find();
        if(!l.verify()) {
            final String lastversion = Preferences.instance().getProperty("donate.reminder");
            if(NSBundle.mainBundle().infoDictionary().objectForKey("CFBundleShortVersionString").toString().equals(lastversion)) {
                // Do not display if same version is installed
                return NSApplication.NSTerminateNow;
            }
            final Calendar nextreminder = Calendar.getInstance();
            nextreminder.setTimeInMillis(Preferences.instance().getLong("donate.reminder.date"));
            // Display donationPrompt every n days
            nextreminder.add(Calendar.DAY_OF_YEAR, Preferences.instance().getInteger("y"));
            log.debug("Next reminder:" + nextreminder.getTime().toString());
            // Display after upgrade
            if(nextreminder.getTime().after(new Date(System.currentTimeMillis()))) {
                // Do not display if shown in the reminder interval
                return NSApplication.NSTerminateNow;
            }
            // Make sure prompt is not loaded twice upon next quit event
            displayDonationPrompt = false;
            final int uses = Preferences.instance().getInteger("uses");
            donationController = new WindowController() {
                @Override
                protected String getBundleName() {
                    return "Donate";
                }

                @Outlet
                private NSButton neverShowDonationCheckbox;

                public void setNeverShowDonationCheckbox(NSButton neverShowDonationCheckbox) {
                    this.neverShowDonationCheckbox = neverShowDonationCheckbox;
                    this.neverShowDonationCheckbox.setTarget(this.id());
                    this.neverShowDonationCheckbox.setState(
                            Preferences.instance().getProperty("donate.reminder").equals(
                                    NSBundle.mainBundle().infoDictionary().objectForKey("CFBundleShortVersionString").toString())
                                    ? NSCell.NSOnState : NSCell.NSOffState);
                }

                @Override
                public void awakeFromNib() {
                    this.window().setTitle(this.window().title() + " (" + uses + ")");
                    this.window().center();
                    this.window().makeKeyAndOrderFront(null);

                    super.awakeFromNib();
                }

                public void closeDonationSheet(final NSButton sender) {
                    if(sender.tag() == SheetCallback.DEFAULT_OPTION) {
                        openUrl(Preferences.instance().getProperty("website.donate"));
                    }
                    this.terminate();
                }

                @Override
                public void windowWillClose(NSNotification notification) {
                    this.terminate();
                    super.windowWillClose(notification);
                }

                private void terminate() {
                    if(neverShowDonationCheckbox.state() == NSCell.NSOnState) {
                        Preferences.instance().setProperty("donate.reminder",
                                NSBundle.mainBundle().infoDictionary().objectForKey("CFBundleShortVersionString").toString());
                    }
                    // Remeber this reminder date
                    Preferences.instance().setProperty("donate.reminder.date", System.currentTimeMillis());
                    // Quit again
                    app.replyToApplicationShouldTerminate(true);
                }
            };
            donationController.loadBundle();
            // Delay application termination. Dismissing the donation dialog will reply to quit.
            return NSApplication.NSTerminateLater;
        }
        return NSApplication.NSTerminateNow;
    }

    /**
     * Quits the Rendezvous daemon and saves all preferences
     *
     * @param notification Notification name
     */
    @Override
    public void applicationWillTerminate(NSNotification notification) {
        log.debug("applicationWillTerminate");

        this.invalidate();

        if(Preferences.instance().getBoolean("rendezvous.enable")) {
            //Terminating rendezvous discovery
            RendezvousFactory.instance().quit();
        }
        //Writing usage info
        Preferences.instance().setProperty("uses", Preferences.instance().getInteger("uses") + 1);
        Preferences.instance().save();
    }

    public void applicationWillRestartAfterUpdate(ID updater) {
        // Disable donation prompt after udpate install
        displayDonationPrompt = false;
    }

    /**
     * Posted when the user has requested a logout or that the machine be powered off.
     *
     * @param notification Notification name
     */
    public void workspaceWillPowerOff(NSNotification notification) {
        log.debug("workspaceWillPowerOff");
    }

    /**
     * Posted before a user session is switched out. This allows an application to
     * disable some processing when its user session is switched out, and reenable when that
     * session gets switched back in, for example.
     *
     * @param notification Notification name
     */
    public void workspaceWillLogout(NSNotification notification) {
        log.debug("workspaceWillLogout");
    }

    public void workspaceWillSleep(NSNotification notification) {
        log.debug("workspaceWillSleep");
    }

    /**
     * Makes a unmounted browser window the key window and brings it to the front
     *
     * @return A reference to a browser window
     */
    public static BrowserController newDocument() {
        return MainController.newDocument(false);
    }

    /**
     *
     */
    private static List<BrowserController> browsers
            = new ArrayList<BrowserController>();

    public static List<BrowserController> getBrowsers() {
        return browsers;
    }

    /**
     * Browser with key focus
     *
     * @return Null if no browser window is open
     */
    public static BrowserController getBrowser() {
        for(BrowserController browser : MainController.getBrowsers()) {
            if(browser.window().isKeyWindow()) {
                return browser;
            }
        }
        return null;
    }


    /**
     * Makes a unmounted browser window the key window and brings it to the front
     *
     * @param force If true, open a new browser regardeless of any unused browser window
     * @return A reference to a browser window
     */
    public static BrowserController newDocument(boolean force) {
        log.debug("newDocument");
        final List<BrowserController> browsers = MainController.getBrowsers();
        if(!force) {
            for(BrowserController controller : browsers) {
                if(!controller.hasSession()) {
                    controller.window().makeKeyAndOrderFront(null);
                    return controller;
                }
            }
        }
        final BrowserController controller = new BrowserController();
        controller.addListener(new WindowListener() {
            @Override
            public void windowWillClose() {
                browsers.remove(controller);
            }
        });
        if(!browsers.isEmpty()) {
            controller.cascade();
        }
        controller.window().makeKeyAndOrderFront(null);
        browsers.add(controller);
        return controller;
    }

    /**
     * We are not a Windows application. Long live the application wide menu bar.
     */
    @Override
    public boolean applicationShouldTerminateAfterLastWindowClosed(NSApplication app) {
        return false;
    }

    /**
     * @return The available character sets available on this platform
     */
    public static String[] availableCharsets() {
        List<String> charsets = new Collection<String>();
        for(Charset charset : Charset.availableCharsets().values()) {
            final String name = charset.displayName();
            if(!(name.startsWith("IBM") || name.startsWith("x-"))) {
                charsets.add(name);
            }
        }
        return charsets.toArray(new String[charsets.size()]);
    }

    @Override
    protected String getBundleName() {
        return "Main";
    }
}