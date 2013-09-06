// 
// Copyright (c) 2010-2013 Yves Langisch. All rights reserved.
// http://cyberduck.ch/
// 
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
// 
// Bug fixes, suggestions and comments should be sent to:
// yves@cyberduck.ch
// 

using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Collections.Specialized;
using System.IO;
using System.Threading;
using System.Windows.Forms;
using BrightIdeasSoftware;
using Ch.Cyberduck.Core;
using Ch.Cyberduck.Ui.Controller.Threading;
using Ch.Cyberduck.Ui.Winforms.Taskdialog;
using StructureMap;
using ch.cyberduck.core;
using ch.cyberduck.core.editor;
using ch.cyberduck.core.io;
using ch.cyberduck.core.local;
using ch.cyberduck.core.serializer;
using ch.cyberduck.core.sftp;
using ch.cyberduck.core.ssl;
using ch.cyberduck.core.threading;
using ch.cyberduck.core.transfer;
using ch.cyberduck.core.transfer.copy;
using ch.cyberduck.core.transfer.download;
using ch.cyberduck.core.transfer.move;
using ch.cyberduck.core.transfer.synchronisation;
using ch.cyberduck.core.transfer.upload;
using ch.cyberduck.ui;
using java.lang;
using java.security.cert;
using java.util;
using org.apache.log4j;
using Application = ch.cyberduck.core.local.Application;
using Collection = ch.cyberduck.core.Collection;
using DataObject = System.Windows.Forms.DataObject;
using Exception = System.Exception;
using Locale = ch.cyberduck.core.i18n.Locale;
using Object = System.Object;
using Path = ch.cyberduck.core.Path;
using String = System.String;
using StringBuilder = System.Text.StringBuilder;
using Timer = System.Threading.Timer;

namespace Ch.Cyberduck.Ui.Controller
{
    public class BrowserController : WindowController<IBrowserView>, TranscriptListener, CollectionListener
    {
        public delegate void CallbackDelegate();

        public delegate bool DialogCallbackDelegate(DialogResult result);

        internal static readonly PathFilter HiddenFilter = new HiddenFilesPathFilter();

        private static readonly Logger Log = Logger.getLogger(typeof (BrowserController).FullName);
        private static readonly PathFilter NullFilter = new NullPathFilter();
        protected static string DEFAULT = Locale.localizedString("Default");
        private readonly List<Path> _backHistory = new List<Path>();
        private readonly BookmarkCollection _bookmarkCollection = BookmarkCollection.defaultCollection();
        private readonly BookmarkModel _bookmarkModel;
        private readonly TreeBrowserModel _browserModel;
        private readonly List<Path> _forwardHistory = new List<Path>();
        private readonly IList<FileSystemWatcher> _temporaryWatcher = new List<FileSystemWatcher>();
        private Comparator _comparator = new NullComparator();
        private InfoController _inspector;
        private BrowserView _lastBookmarkView = BrowserView.Bookmark;
        private ConnectionListener _listener;
        private ProgessListener _progress;

        /*
         * No file filter.
         */
        private Session _session;
        private bool _sessionShouldBeConnected;
        private bool _showHiddenFiles;
        private Path _workdir;
        private String dropFolder; // holds the drop folder of the current drag operation

        public BrowserController(IBrowserView view)
        {
            View = view;

            ShowHiddenFiles = Preferences.instance().getBoolean("browser.showHidden");

            _browserModel = new TreeBrowserModel(this);
            _bookmarkModel = new BookmarkModel(this, BookmarkCollection.defaultCollection());

            //default view is the bookmark view
            ToggleView(BrowserView.Bookmark);
            View.StopActivityAnimation();

            View.SetComparator += View_SetComparator;
            View.ChangeBrowserView += View_ChangeBrowserView;

            View.QuickConnect += View_QuickConnect;
            View.BrowserDoubleClicked += View_BrowserDoubleClicked;
            View.ViewShownEvent += ViewViewShownEvent;
            View.BrowserSelectionChanged += View_BrowserSelectionChanged;
            View.PathSelectionChanged += View_PathSelectionChanged;
            View.EditEvent += View_EditEvent;
            View.ItemsChanged += View_ItemsChanged;

            View.ShowTransfers += View_ShowTransfers;

            View.BrowserCanDrop += View_BrowserCanDrop;
            View.HostCanDrop += View_HostCanDrop;
            View.BrowserModelCanDrop += View_BrowserModelCanDrop;
            View.HostModelCanDrop += View_HostModelCanDrop;
            View.BrowserDropped += View_BrowserDropped;
            View.HostDropped += View_HostDropped;
            View.HostModelDropped += View_HostModelDropped;
            View.BrowserModelDropped += View_BrowserModelDropped;
            View.BrowserDrag += View_BrowserDrag;
            View.HostDrag += View_HostDrag;
            View.BrowserEndDrag += View_BrowserEndDrag;
            View.HostEndDrag += View_HostEndDrag;
            View.SearchFieldChanged += View_SearchFieldChanged;


            View.ContextMenuEnabled += View_ContextMenuEnabled;

            #region Commands - File

            View.NewBrowser += View_NewBrowser;
            View.ValidateNewBrowser += View_ValidateNewBrowser;
            View.OpenConnection += View_OpenConnection;
            View.ValidateOpenConnection += () => true;
            View.NewDownload += View_NewDownload;
            View.ValidateNewDownload += () => false; //todo implement
            View.NewFolder += View_NewFolder;
            View.ValidateNewFolder += View_ValidateNewFolder;
            View.NewFile += View_NewFile;
            View.ValidateNewFile += View_ValidateNewFile;
            View.NewSymbolicLink += View_NewSymbolicLink;
            View.ValidateNewSymbolicLink += View_ValidateNewSymbolicLink;
            View.RenameFile += View_RenameFile;
            View.ValidateRenameFile += View_ValidateRenameFile;
            View.DuplicateFile += View_DuplicateFile;
            View.ValidateDuplicateFile += View_ValidateDuplicateFile;
            View.OpenUrl += View_OpenUrl;
            View.ValidateOpenWebUrl += View_ValidateOpenWebUrl;
            View.ValidateEditWith += View_ValidateEditWith;
            View.ShowInspector += View_ShowInspector;
            View.ValidateShowInspector += View_ValidateShowInspector;
            View.Download += View_Download;
            View.ValidateDownload += View_ValidateDownload;
            View.DownloadAs += View_DownloadAs;
            View.ValidateDownloadAs += View_ValidateDownloadAs;
            View.DownloadTo += View_DownloadTo;
            View.ValidateDownloadTo += View_ValidateDownload; //use same validation handler
            View.Upload += View_Upload;
            View.ValidateUpload += View_ValidateUpload;
            View.Synchronize += View_Synchronize;
            View.ValidateSynchronize += View_ValidateSynchronize;
            View.Delete += View_Delete;
            View.ValidateDelete += View_ValidateDelete;
            View.RevertFile += View_RevertFile;
            View.ValidateRevertFile += View_ValidateRevertFile;
            View.GetArchives += View_GetArchives;
            View.GetCopyUrls += View_GetCopyUrls;
            View.GetOpenUrls += View_GetOpenUrls;
            View.CreateArchive += View_CreateArchive;
            View.ValidateCreateArchive += View_ValidateCreateArchive;
            View.ExpandArchive += View_ExpandArchive;
            View.ValidateExpandArchive += View_ValidateExpandArchive;

            #endregion

            #region Commands - Edit

            View.Cut += View_Cut;
            View.ValidateCut += View_ValidateCut;
            View.Copy += View_Copy;
            View.ValidateCopy += View_ValidateCopy;
            View.Paste += View_Paste;
            View.ValidatePaste += View_ValidatePaste;
            View.ShowPreferences += View_ShowPreferences;

            #endregion

            #region Commands - View

            View.ToggleToolbar += View_ToggleToolbar;
            View.ShowHiddenFiles += View_ShowHiddenFiles;
            View.ValidateTextEncoding += View_ValidateTextEncoding;
            View.EncodingChanged += View_EncodingChanged;
            View.ToggleLogDrawer += View_ToggleLogDrawer;

            #endregion

            #region Commands - Go

            View.RefreshBrowser += View_RefreshBrowser;
            View.ValidateRefresh += View_ValidateRefresh;
            View.GotoFolder += View_GotoFolder;
            View.ValidateGotoFolder += View_ValidateGotoFolder;
            View.HistoryBack += View_HistoryBack;
            View.ValidateHistoryBack += View_ValidateHistoryBack;
            View.HistoryForward += View_HistoryForward;
            View.ValidateHistoryForward += View_ValidateHistoryForward;
            View.FolderUp += View_FolderUp;
            View.ValidateFolderUp += View_ValidateFolderUp;
            View.FolderInside += View_FolderInside;
            View.ValidateFolderInside += View_ValidateFolderInside;
            View.Search += View_Search;
            View.SendCustomCommand += View_SendCustomCommand;
            View.ValidateSendCustomCommand += View_ValidateSendCustomCommand;
            View.OpenInTerminal += View_OpenInTerminal;
            View.ValidateOpenInTerminal += View_ValidateOpenInTerminal;
            View.Stop += View_Stop;
            View.ValidateStop += View_ValidateStop;
            View.Disconnect += View_Disconnect;
            View.ValidateDisconnect += View_ValidateDisconnect;

            #endregion

            #region Commands - Bookmark

            View.ToggleBookmarks += View_ToggleBookmarks;
            View.SortBookmarksByHostname += View_SortBookmarksByHostname;
            View.SortBookmarksByNickname += View_SortBookmarksByNickname;
            View.SortBookmarksByProtocol += View_SortBookmarksByProtocol;

            View.ConnectBookmark += View_ConnectBookmark;
            View.ValidateConnectBookmark += View_ValidateConnectBookmark;
            View.NewBookmark += View_NewBookmark;
            View.ValidateNewBookmark += View_ValidateNewBookmark;
            View.EditBookmark += View_EditBookmark;
            View.ValidateEditBookmark += View_ValidateEditBookmark;
            View.DeleteBookmark += View_DeleteBookmark;
            View.ValidateDeleteBookmark += View_ValidateDeleteBookmark;
            View.DuplicateBookmark += View_DuplicateBookmark;
            View.ValidateDuplicateBookmark += View_ValidateDuplicateBookmark;

            #endregion

            #region Browser model delegates

            View.ModelCanExpandDelegate = _browserModel.CanExpand;
            View.ModelChildrenGetterDelegate = _browserModel.ChildrenGetter;
            View.ModelFilenameGetter = _browserModel.GetName;
            View.ModelIconGetter = _browserModel.GetIcon;
            View.ModelSizeGetter = _browserModel.GetSize;
            View.ModelSizeAsStringGetter = _browserModel.GetSizeAsString;
            View.ModelModifiedGetter = _browserModel.GetModified;
            View.ModelModifiedAsStringGetter = _browserModel.GetModifiedAsString;
            View.ModelOwnerGetter = _browserModel.GetOwner;
            View.ModelGroupGetter = _browserModel.GetGroup;
            View.ModelPermissionsGetter = _browserModel.GetPermission;
            View.ModelKindGetter = _browserModel.GetKind;
            View.ModelActiveGetter = _browserModel.GetActive;

            #endregion

            #region Bookmark model delegates

            View.BookmarkImageGetter = _bookmarkModel.GetBookmarkImage;
            View.BookmarkNicknameGetter = _bookmarkModel.GetNickname;
            View.BookmarkHostnameGetter = _bookmarkModel.GetHostname;
            View.BookmarkUrlGetter = _bookmarkModel.GetUrl;
            View.BookmarkNotesGetter = _bookmarkModel.GetNotes;
            View.BookmarkStatusImageGetter = _bookmarkModel.GetBookmarkStatusImage;

            #endregion

            _bookmarkCollection.addListener(this);

            PopulateQuickConnect();
            PopulateEncodings();
            UpdateOpenIcon();

            View.ToolbarVisible = Preferences.instance().getBoolean("browser.toolbar");
            View.LogDrawerVisible = Preferences.instance().getBoolean("browser.logDrawer.isOpen");

            View.GetEditorsForSelection += View_GetEditorsForSelection;
            View.GetBookmarks += View_GetBookmarks;
            View.GetHistory += View_GetHistory;
            View.GetBonjourHosts += View_GetBonjourHosts;
            View.ClearHistory += View_ClearHistory;
            View.ShowCertificate += View_Certificate;

            View.ValidatePathsCombobox += View_ValidatePathsCombobox;
            View.ValidateSearchField += View_ValidateSearchField;

            View.Exit += View_Exit;
            View.ViewShownEvent += delegate
                {
                    //eagerly initialize the TransferController singleton
                    TransferController tc = TransferController.Instance;
                };
            BookmarkCollection bookmarkCollection = BookmarkCollection.defaultCollection();
            //todo eigene ListenerKlasse muss her
            //hostCollection.addListener(this);
            View.ViewClosedEvent += delegate { bookmarkCollection.removeListener(this); };
            View.SetBookmarkModel(bookmarkCollection, null);
        }

        public BrowserController()
            : this(ObjectFactory.GetInstance<IBrowserView>())
        {
        }

        /// <summary>
        /// The first selected path found or null if there is no selection
        /// </summary>
        public Path SelectedPath
        {
            get
            {
                IList<Path> selectedPaths = View.SelectedPaths;
                if (selectedPaths.Count > 0)
                {
                    return selectedPaths[0];
                }
                return null;
            }
        }

        public List<Path> BackHistory
        {
            get { return _backHistory; }
        }

        public List<Path> ForwardHistory
        {
            get { return _forwardHistory; }
        }

        public Path Workdir
        {
            get { return _workdir; }
            set { _workdir = value; }
        }

        /// <summary>
        ///
        /// </summary>
        /// <value>
        ///   All selected paths or an empty list if there is no selection
        /// </value>
        public IList<Path> SelectedPaths
        {
            get
            {
                if (IsMounted())
                {
                    return View.SelectedPaths;
                }
                return new List<Path>();
            }
            set
            {
                List<Path> selected = new List<Path>();
                foreach (Path s in SelectedPaths)
                {
                    selected.Add(PathFactory.createPath(getSession(), s.getAsDictionary()));
                }
                View.SelectedPaths = selected;
            }
        }

        public bool ShowHiddenFiles
        {
            get { return _showHiddenFiles; }
            set
            {
                FilenameFilter = value ? NullFilter : HiddenFilter;
                _showHiddenFiles = value;
                View.HiddenFilesVisible = _showHiddenFiles;
            }
        }

        public PathFilter FilenameFilter { get; set; }

        public Comparator FilenameComparator
        {
            get { return _comparator; }
            set { _comparator = value; }
        }

        public bool SessionShouldBeConnected
        {
            set { _sessionShouldBeConnected = value; }
        }

        public void collectionLoaded()
        {
            AsyncDelegate mainAction = delegate { ReloadBookmarks(); };
            Invoke(mainAction);
        }

        public void collectionItemAdded(object obj)
        {
            AsyncDelegate mainAction = delegate { PopulateQuickConnect(); };
            Invoke(mainAction);
        }

        public void collectionItemRemoved(object obj)
        {
            AsyncDelegate mainAction = delegate { PopulateQuickConnect(); };
            Invoke(mainAction);
        }

        public void collectionItemChanged(object obj)
        {
            AsyncDelegate mainAction = delegate { PopulateQuickConnect(); };
            Invoke(mainAction);
        }

        public void log(bool request, string transcript)
        {
            if (View.LogDrawerVisible)
            {
                AsyncDelegate mainAction = delegate { View.AddTranscriptEntry(request, transcript); };
                Invoke(mainAction);
            }
        }

        private void View_NewSymbolicLink()
        {
            CreateSymlinkController slc =
                new CreateSymlinkController(ObjectFactory.GetInstance<ICreateSymlinkPromptView>(), this);
            slc.Show();
        }

        private bool View_ValidateNewSymbolicLink()
        {
            return IsMounted() && getSession().isCreateSymlinkSupported() && SelectedPaths.Count == 1;
        }

        private void View_SortBookmarksByProtocol()
        {
            BookmarkCollection.defaultCollection().sortByProtocol();
            ReloadBookmarks();
        }

        private void View_SortBookmarksByNickname()
        {
            BookmarkCollection.defaultCollection().sortByNickname();
            ReloadBookmarks();
        }

        private void View_SortBookmarksByHostname()
        {
            BookmarkCollection.defaultCollection().sortByHostname();
            ReloadBookmarks();
        }

        private bool View_ValidateOpenInTerminal()
        {
            return IsMounted() && getSession() is SFTPSession &&
                   File.Exists(Preferences.instance().getProperty("terminal.command.ssh"));
        }

        private void View_OpenInTerminal()
        {
            Host host = getSession().getHost();
            bool identity = host.getCredentials().isPublicKeyAuthentication();

            String workdir = null;
            if (SelectedPaths.Count == 1)
            {
                Path selected = SelectedPath;
                if (selected.attributes().isDirectory())
                {
                    workdir = selected.getAbsolute();
                }
            }
            if (null == workdir)
            {
                workdir = Workdir.getAbsolute();
            }

            string tempFile = System.IO.Path.GetTempFileName();
            TextWriter tw = new StreamWriter(tempFile);
            tw.WriteLine(String.Format("cd {0} && exec $SHELL", workdir));
            tw.Close();

            String ssh = String.Format(Preferences.instance().getProperty("terminal.command.ssh.args"),
                                       identity
                                           ? "-i " + host.getCredentials().getIdentity().getAbsolute()
                                           : String.Empty,
                                       host.getCredentials().getUsername(),
                                       host.getHostname(),
                                       Convert.ToString(host.getPort()), tempFile);


            ApplicationLauncherFactory.get().open(
                new Application(Preferences.instance().getProperty("terminal.command.ssh"), null), ssh);
        }

        private void View_SetComparator(BrowserComparator comparator)
        {
            if (!comparator.equals(_comparator))
            {
                _comparator = comparator;
                ReloadData(true);
            }
        }

        private IList<Application> View_GetEditorsForSelection()
        {
            Path p = SelectedPath;
            if (null != p)
            {
                if (p.attributes().isFile())
                {
                    return Utils.ConvertFromJavaList<Application>(EditorFactory.instance().getEditors(p.getName()), null);
                }
            }
            return new List<Application>();
        }

        private bool View_ValidateNewBrowser()
        {
            return IsMounted();
        }

        private List<KeyValuePair<String, List<String>>> View_GetCopyUrls()
        {
            List<KeyValuePair<String, List<String>>> items = new List<KeyValuePair<String, List<String>>>();
            IList<Path> selected = View.SelectedPaths;
            if (selected.Count == 0)
            {
                items.Add(
                    new KeyValuePair<string, List<String>>(Locale.localizedString("None"),
                                                           new List<string>()));
            }
            else
            {
                for (int i = 0; i < SelectedPath.getURLs().size(); i++)
                {
                    DescriptiveUrl descUrl =
                        (DescriptiveUrl) SelectedPath.getURLs().toArray()[i];
                    KeyValuePair<String, List<String>> entry =
                        new KeyValuePair<string, List<string>>(descUrl.getHelp(), new List<string>());
                    items.Add(entry);

                    foreach (Path path in selected)
                    {
                        entry.Value.Add(((DescriptiveUrl) path.getURLs().toArray()[i]).getUrl());
                    }
                }
            }
            return items;
        }

        private IList<KeyValuePair<string, List<string>>> View_GetOpenUrls()
        {
            IList<KeyValuePair<String, List<String>>> items = new List<KeyValuePair<String, List<String>>>();
            IList<Path> selected = View.SelectedPaths;
            if (selected.Count == 0)
            {
                items.Add(
                    new KeyValuePair<string, List<String>>(
                        Locale.localizedString("None"),
                        new List<string>()));
            }
            else
            {
                for (int i = 0; i < SelectedPath.getHttpURLs().size(); i++)
                {
                    DescriptiveUrl descUrl =
                        (DescriptiveUrl) SelectedPath.getHttpURLs().toArray()[i];
                    KeyValuePair<String, List<String>> entry =
                        new KeyValuePair<string, List<string>>(descUrl.getHelp(), new List<string>());
                    items.Add(entry);

                    foreach (Path path in selected)
                    {
                        entry.Value.Add(
                            ((DescriptiveUrl) path.getHttpURLs().toArray()[i]).getUrl());
                    }
                }
            }
            return items;
        }

        private bool View_ValidateDuplicateBookmark()
        {
            return _bookmarkModel.Source.allowsEdit() && View.SelectedBookmarks.Count == 1;
        }

        private void View_DuplicateBookmark()
        {
            ToggleView(BrowserView.Bookmark);
            Host duplicate = new Host(View.SelectedBookmark.getAsDictionary());
            // Make sure a new UUID is asssigned for duplicate
            duplicate.setUuid(null);
            AddBookmark(duplicate);
        }

        private void View_HostModelDropped(ModelDropEventArgs dropargs)
        {
            int sourceIndex = _bookmarkModel.Source.indexOf(dropargs.SourceModels[0]);
            int destIndex = dropargs.DropTargetIndex;
            if (dropargs.DropTargetLocation == DropTargetLocation.BelowItem)
            {
                destIndex++;
            }
            if (dropargs.Effect == DragDropEffects.Copy)
            {
                Host host = new Host(((Host) dropargs.SourceModels[0]).getAsDictionary());
                host.setUuid(null);
                AddBookmark(host, destIndex);
            }
            if (dropargs.Effect == DragDropEffects.Move)
            {
                if (sourceIndex < destIndex)
                {
                    destIndex--;
                }
                foreach (Host promisedDragBookmark in dropargs.SourceModels)
                {
                    _bookmarkModel.Source.remove(promisedDragBookmark);
                    if (destIndex > _bookmarkModel.Source.size())
                    {
                        _bookmarkModel.Source.add(promisedDragBookmark);
                    }
                    else
                    {
                        _bookmarkModel.Source.add(destIndex, promisedDragBookmark);
                    }
                    //view.selectRowIndexes(NSIndexSet.indexSetWithIndex(row), false);
                    //view.scrollRowToVisible(row);
                }
            }
        }

        private void View_HostModelCanDrop(ModelDropEventArgs args)
        {
            if (!_bookmarkModel.Source.allowsEdit())
            {
                // Do not allow drags for non writable collections
                args.Effect = DragDropEffects.None;
                args.DropTargetLocation = DropTargetLocation.None;
                return;
            }
            switch (args.DropTargetLocation)
            {
                case DropTargetLocation.BelowItem:
                case DropTargetLocation.AboveItem:
                    if (args.SourceModels.Count > 1)
                    {
                        args.Effect = DragDropEffects.Move;
                    }
                    break;
                default:
                    args.Effect = DragDropEffects.None;
                    args.DropTargetLocation = DropTargetLocation.None;
                    return;
            }
        }

        private void View_HostDropped(OlvDropEventArgs e)
        {
            if (e.DataObject is DataObject && ((DataObject) e.DataObject).ContainsFileDropList())
            {
                DataObject data = (DataObject) e.DataObject;

                if (e.DropTargetLocation == DropTargetLocation.Item)
                {
                    foreach (string file in data.GetFileDropList())
                    {
                        //check if we received at least one non-duck file
                        if (!".duck".Equals(Utils.GetSafeExtension(file)))
                        {
                            // The bookmark this file has been dropped onto
                            Host destination = (Host) e.DropTargetItem.RowObject;
                            IList<Path> roots = new List<Path>();
                            Session session = null;
                            foreach (string upload in data.GetFileDropList())
                            {
                                if (null == session)
                                {
                                    session = SessionFactory.createSession(destination);
                                }
                                // Upload to the remote host this bookmark points to
                                roots.Add(PathFactory.createPath(session,
                                                                 destination.getDefaultPath(),
                                                                 LocalFactory.createLocal(upload)));
                            }
                            if (roots.Count > 0)
                            {
                                UploadTransfer q = new UploadTransfer(Utils.ConvertToJavaList(roots));
                                // If anything has been added to the queue, then process the queue
                                if (q.numberOfRoots() > 0)
                                {
                                    TransferController.Instance.StartTransfer(q);
                                }
                            }
                        }
                    }
                    return;
                }

                if (e.DropTargetLocation == DropTargetLocation.AboveItem)
                {
                    Host destination = (Host) e.DropTargetItem.RowObject;
                    foreach (string file in data.GetFileDropList())
                    {
                        _bookmarkModel.Source.add(_bookmarkModel.Source.indexOf(destination),
                                                  HostReaderFactory.get().read(LocalFactory.createLocal(file)));
                    }
                }
                if (e.DropTargetLocation == DropTargetLocation.BelowItem)
                {
                    Host destination = (Host) e.DropTargetItem.RowObject;
                    foreach (string file in data.GetFileDropList())
                    {
                        _bookmarkModel.Source.add(_bookmarkModel.Source.indexOf(destination) + 1,
                                                  HostReaderFactory.get().read(LocalFactory.createLocal(file)));
                    }
                }
                if (e.DropTargetLocation == DropTargetLocation.Background)
                {
                    foreach (string file in data.GetFileDropList())
                    {
                        _bookmarkModel.Source.add(HostReaderFactory.get().read(LocalFactory.createLocal(file)));
                    }
                }
            }
        }

        private void View_HostCanDrop(OlvDropEventArgs args)
        {
            if (!_bookmarkModel.Source.allowsEdit())
            {
                // Do not allow drags for non writable collections
                args.Effect = DragDropEffects.None;
                args.DropTargetLocation = DropTargetLocation.None;
                return;
            }

            DataObject dataObject = (DataObject) args.DataObject;
            if (dataObject.ContainsFileDropList())
            {
                //check if all files are .duck files
                foreach (string file in dataObject.GetFileDropList())
                {
                    string ext = Utils.GetSafeExtension(file);
                    if (!".duck".Equals(ext))
                    {
                        //if at least one non-duck file we prepare for uploading
                        args.Effect = DragDropEffects.Copy;
                        if (args.DropTargetLocation == DropTargetLocation.Item)
                        {
                            Host destination = (Host) args.DropTargetItem.RowObject;
                            (args.DataObject as DataObject).SetDropDescription((DropImageType) args.Effect,
                                                                               "Upload to %1",
                                                                               destination.getNickname());
                        }
                        args.DropTargetLocation = DropTargetLocation.Item;
                        return;
                    }
                }

                //at least one .duck file
                args.Effect = DragDropEffects.Copy;
                if (args.DropTargetLocation == DropTargetLocation.Item)
                {
                    args.DropTargetLocation = DropTargetLocation.Background;
                }
                return;
            }
            args.Effect = DragDropEffects.None;
        }

        private void View_HostEndDrag(DataObject data)
        {
            RemoveTemporaryFiles(data);
            RemoveTemporaryFilesystemWatcher();
        }

        private string CreateAndWatchTemporaryFile(FileSystemEventHandler del)
        {
            string tfile = System.IO.Path.Combine(System.IO.Path.GetTempPath(),
                                                  Guid.NewGuid().ToString());
            using (File.Create(tfile))
            {
                FileInfo tmpFile = new FileInfo(tfile);
                tmpFile.Attributes |= FileAttributes.Hidden;
            }
            DriveInfo[] allDrives = DriveInfo.GetDrives();
            foreach (DriveInfo d in allDrives)
            {
                if (d.IsReady && d.DriveType != DriveType.CDRom)
                {
                    try
                    {
                        FileSystemWatcher watcher = new FileSystemWatcher(@d.Name, System.IO.Path.GetFileName(tfile));
                        watcher.BeginInit();
                        watcher.IncludeSubdirectories = true;
                        watcher.EnableRaisingEvents = true;
                        watcher.Created += del;
                        watcher.EndInit();
                        _temporaryWatcher.Add(watcher);
                    }
                    catch (Exception e)
                    {
                        Log.info(string.Format("Cannot watch drive {0}", d), e);
                    }
                }
            }
            return tfile;
        }

        private DataObject View_HostDrag(ObjectListView list)
        {
            DataObject data = new DataObject(DataFormats.FileDrop, new[]
                {
                    CreateAndWatchTemporaryFile(
                        delegate(object sender,
                                 FileSystemEventArgs args)
                            {
                                Invoke(delegate
                                    {
                                        dropFolder =
                                            System.IO.Path
                                                  .
                                                   GetDirectoryName
                                                (
                                                    args.
                                                        FullPath);
                                        foreach (
                                            Host host in
                                                View.
                                                    SelectedBookmarks
                                            )
                                        {
                                            string
                                                filename =
                                                    host.
                                                        getNickname
                                                        () +
                                                    ".duck";
                                            foreach (
                                                char c in
                                                    System.IO.Path
                                                          .
                                                           GetInvalidFileNameChars
                                                        ()
                                                )
                                            {
                                                filename =
                                                    filename
                                                        .
                                                        Replace
                                                        (
                                                            c
                                                                .
                                                                ToString
                                                                (),
                                                            String.Empty);
                                            }

                                            Local file =
                                                LocalFactory
                                                    .
                                                    createLocal
                                                    (
                                                        dropFolder,
                                                        filename);
                                            HostWriterFactory
                                                .get()
                                                .
                                                 write(
                                                     host,
                                                     file);
                                        }
                                    });
                            }
                                                                       )
                });
            return data;
        }

        private void View_BrowserModelCanDrop(ModelDropEventArgs args)
        {
            if (IsMounted())
            {
                Path destination;
                switch (args.DropTargetLocation)
                {
                    case DropTargetLocation.Item:
                        destination = (Path) args.DropTargetItem.RowObject;
                        if (!destination.attributes().isDirectory())
                        {
                            //dragging over file
                            destination = destination.getParent();
                        }
                        break;
                    case DropTargetLocation.Background:
                        destination = Workdir;
                        break;
                    default:
                        args.Effect = DragDropEffects.None;
                        args.DropTargetLocation = DropTargetLocation.None;
                        return;
                }
                if (!getSession().isCreateFileSupported(destination))
                {
                    args.Effect = DragDropEffects.None;
                    args.DropTargetLocation = DropTargetLocation.None;
                    return;
                }
                foreach (Path sourcePath in args.SourceModels)
                {
                    if (destination.getSession().equals(sourcePath.getSession()))
                    {
                        // Use drag action from user
                    }
                    else
                    {
                        // If copying between sessions is supported
                        args.Effect = DragDropEffects.Copy;
                    }
                    if (sourcePath.attributes().isDirectory() && sourcePath.equals(destination))
                    {
                        // Do not allow dragging onto myself.
                        args.Effect = DragDropEffects.None;
                        args.DropTargetLocation = DropTargetLocation.None;
                        return;
                    }
                    if (sourcePath.attributes().isDirectory() && destination.isChild(sourcePath))
                    {
                        // Do not allow dragging a directory into its own containing items
                        args.Effect = DragDropEffects.None;
                        args.DropTargetLocation = DropTargetLocation.None;
                        return;
                    }
                    if (sourcePath.attributes().isFile() && sourcePath.getParent().equals(destination))
                    {
                        // Moving file to the same destination makes no sense
                        args.Effect = DragDropEffects.None;
                        args.DropTargetLocation = DropTargetLocation.None;
                        return;
                    }
                }
                if (Workdir == destination)
                {
                    args.DropTargetLocation = DropTargetLocation.Background;
                }
                else
                {
                    args.DropTargetItem = args.ListView.ModelToItem(destination);
                }
            }
        }

        /// <summary>
        /// A file dragged within the browser has been received
        /// </summary>
        /// <param name="dropargs"></param>
        private void View_BrowserModelDropped(ModelDropEventArgs dropargs)
        {
            Path destination;
            switch (dropargs.DropTargetLocation)
            {
                case DropTargetLocation.Item:
                    destination = (Path) dropargs.DropTargetItem.RowObject;
                    break;
                case DropTargetLocation.Background:
                    destination = Workdir;
                    break;
                default:
                    destination = null;
                    break;
            }

            if (null != destination)
            {
                IDictionary<Path, Path> files = new Dictionary<Path, Path>();
                if (dropargs.Effect == DragDropEffects.Copy)
                {
                    // Drag to browser windows with different session or explicit copy requested by user.
                    Session target = getTransferSession();
                    foreach (Path path in dropargs.SourceModels)
                    {
                        Session source = SessionFactory.createSession(path.getSession().getHost());
                        Path next = PathFactory.createPath(source, path.getAsDictionary());
                        Path renamed = PathFactory.createPath(target, destination.getAbsolute(), next.getName(),
                                                              next.attributes().getType());
                        files.Add(next, renamed);
                    }
                    DuplicatePaths(files, dropargs.SourceListView == dropargs.ListView);
                }
                if (dropargs.Effect == DragDropEffects.Move)
                {
                    Session session = getSession();
                    // The file should be renamed
                    foreach (Path path in dropargs.SourceModels)
                    {
                        Path next = PathFactory.createPath(session, path.getAsDictionary());
                        Path renamed = PathFactory.createPath(session, destination.getAbsolute(), next.getName(),
                                                              next.attributes().getType());
                        files.Add(next, renamed);
                    }
                    RenamePaths(files);
                }
            }
        }

        private void View_Download()
        {
            Download(SelectedPaths);
        }

        private bool View_ValidateRevertFile()
        {
            if (IsMounted() && SelectedPaths.Count == 1)
            {
                return getSession().isRevertSupported();
            }
            return false;
        }

        private void View_RevertFile()
        {
            RevertPath(SelectedPath);
        }

        private void RevertPath(Path selected)
        {
            Background(new RevertPathAction(this, selected));
        }

        private void View_ToggleBookmarks()
        {
            if (View.CurrentView == BrowserView.File)
            {
                View.CurrentView = _lastBookmarkView;
            }
            else
            {
                _lastBookmarkView = View.CurrentView;
                View.CurrentView = BrowserView.File;
            }
        }

        private bool View_ValidateSearchField()
        {
            return IsMounted() || View.CurrentView != BrowserView.File;
        }

        private bool View_ValidatePathsCombobox()
        {
            return IsMounted();
        }

        private void View_ItemsChanged()
        {
            UpdateStatusLabel();
        }

        private void View_Certificate()
        {
            if (_session is SSLSession)
            {
                X509Certificate[] certificates =
                    ((SSLSession) _session).getTrustManager().getAcceptedIssuers();
                if (0 == certificates.Length)
                {
                    Log.warn("No accepted certificates found");
                    return;
                }
                KeychainFactory.get().displayCertificates(certificates);
            }
        }

        private void View_ClearHistory()
        {
            HistoryCollection.defaultCollection().clear();
        }

        private List<Host> View_GetBonjourHosts()
        {
            List<Host> b = new List<Host>();
            foreach (Host h in RendezvousCollection.defaultCollection())
            {
                b.Add(h);
            }
            return b;
        }

        private List<Host> View_GetHistory()
        {
            List<Host> b = new List<Host>();
            foreach (Host h in HistoryCollection.defaultCollection())
            {
                b.Add(h);
            }
            return b;
        }

        private List<Host> View_GetBookmarks()
        {
            List<Host> b = new List<Host>();
            foreach (Host h in BookmarkCollection.defaultCollection())
            {
                b.Add(h);
            }
            return b;
        }

        private void PopulateEncodings()
        {
            List<string> list = new List<string>();
            list.AddRange(Utils.AvailableCharsets());
            View.PopulateEncodings(list);
            View.SelectedEncoding = Preferences.instance().getProperty("browser.charset.encoding");
        }

        private void View_EncodingChanged(object sender, EncodingChangedArgs e)
        {
            string encoding = e.Encoding;
            if (Utils.IsBlank(encoding))
            {
                return;
            }
            View.SelectedEncoding = encoding;
            if (IsMounted())
            {
                if (_session.getEncoding().Equals(encoding))
                {
                    return;
                }
                background(new EncodingBrowserBackgroundAction(this, encoding));
            }
        }

        private void View_ConnectBookmark(object sender, ConnectBookmarkArgs connectBookmarkArgs)
        {
            Mount(connectBookmarkArgs.Bookmark);
        }

        private bool View_ValidateConnectBookmark()
        {
            return View.SelectedBookmarks.Count == 1;
        }

        private bool View_ValidateDeleteBookmark()
        {
            return _bookmarkModel.Source.allowsDelete() && View.SelectedBookmarks.Count > 0;
        }

        private bool View_ValidateEditBookmark()
        {
            return _bookmarkModel.Source.allowsEdit() && View.SelectedBookmarks.Count == 1;
        }

        private bool View_ValidateNewBookmark()
        {
            return _bookmarkModel.Source.allowsAdd();
        }

        private void View_ChangeBrowserView(object sender, ChangeBrowserViewArgs e)
        {
            ToggleView(e.View);
        }

        private void View_EditBookmark()
        {
            if (View.SelectedBookmarks.Count == 1)
            {
                BookmarkController.Factory.Create(View.SelectedBookmark).View.Show(View);
            }
        }

        private void View_NewBookmark()
        {
            Host bookmark;
            if (IsMounted())
            {
                Path selected = SelectedPath;
                if (null == selected || !selected.attributes().isDirectory())
                {
                    selected = Workdir;
                }
                bookmark = new Host(_session.getHost().getAsDictionary());
                bookmark.setUuid(null);
                bookmark.setDefaultPath(selected.getAbsolute());
            }
            else
            {
                bookmark =
                    new Host(
                        ProtocolFactory.forName(
                            Preferences.instance().getProperty("connection.protocol.default")),
                        Preferences.instance().getProperty("connection.hostname.default"),
                        Preferences.instance().getInteger("connection.port.default"));
            }
            ToggleView(BrowserView.Bookmark);
            AddBookmark(bookmark);
        }

        public void AddBookmark(Host item)
        {
            AddBookmark(item, -1);
        }

        private void AddBookmark(Host item, int index)
        {
            _bookmarkModel.Filter = null;
            if (index != -1)
            {
                _bookmarkModel.Source.add(index, item);
            }
            else
            {
                _bookmarkModel.Source.add(item);
            }
            View.SelectBookmark(item);
            View.EnsureBookmarkVisible(item);
            BookmarkController.Factory.Create(item).View.Show(View);
        }

        private void View_DeleteBookmark()
        {
            List<Host> selected = View.SelectedBookmarks;
            StringBuilder alertText = new StringBuilder();
            int i = 0;
            foreach (Host host in selected)
            {
                if (i > 0)
                {
                    alertText.Append("\n");
                }
                alertText.Append(Character.toString('\u2022')).Append(" ").Append(host.getNickname());
                i++;
                if (i > 10)
                {
                    break;
                }
            }
            DialogResult result = QuestionBox(Locale.localizedString("Delete Bookmark"),
                                              Locale.localizedString(
                                                  "Do you want to delete the selected bookmark?"),
                                              alertText.ToString(),
                                              String.Format("{0}", Locale.localizedString("Delete")),
                                              true);
            if (result == DialogResult.OK)
            {
                _bookmarkModel.Source.removeAll(Utils.ConvertToJavaList(selected));
            }
        }

        public override bool ViewShouldClose()
        {
            return Unmount();
        }

        protected override void Invalidate()
        {
            if (HasSession())
            {
                _session.removeProgressListener(_progress);
                _session.removeConnectionListener(_listener);
            }
            _bookmarkCollection.removeListener(this);
        }

        private void View_OpenUrl()
        {
            foreach (Path selected in SelectedPaths)
            {
                Utils.StartProcess(selected.toHttpURL());
            }
        }

        private void View_SearchFieldChanged()
        {
            if (View.CurrentView == BrowserView.File)
            {
                SetPathFilter(View.SearchString);
            }
            else
            {
                SetBookmarkFilter(View.SearchString);
            }
        }

        private void SetBookmarkFilter(string searchString)
        {
            if (Utils.IsBlank(searchString))
            {
                View.SearchString = String.Empty;
                _bookmarkModel.Filter = null;
            }
            else
            {
                _bookmarkModel.Filter = new BookmarkFilter(searchString);
            }
            ReloadBookmarks();
        }

        private bool View_ValidateDisconnect()
        {
            // disconnect/stop button update
            View.ActivityRunning = IsActivityRunning();
            if (!IsConnected())
            {
                return IsActivityRunning();
            }
            return IsConnected();
        }

        private bool View_ValidateStop()
        {
            return IsActivityRunning();
        }

        private bool View_ValidateSendCustomCommand()
        {
            return false;
            return IsMounted() && getSession().isSendCommandSupported(); //todo implement Send custom command
        }

        private bool View_ValidateFolderInside()
        {
            return IsMounted() && SelectedPaths.Count > 0;
        }

        private bool View_ValidateFolderUp()
        {
            return IsMounted() && !Workdir.isRoot();
        }

        private bool View_ValidateHistoryForward()
        {
            return IsMounted() && ForwardHistory.Count > 0;
        }

        private bool View_ValidateHistoryBack()
        {
            return IsMounted() && BackHistory.Count > 1;
        }

        private bool View_ValidateGotoFolder()
        {
            return IsMounted();
        }

        private bool View_ValidateRefresh()
        {
            return IsMounted();
        }

        private void View_Disconnect()
        {
            if (IsActivityRunning())
            {
                View_Stop();
            }
            else
            {
                Disconnect();
            }
        }

        /// <summary>
        /// Unmount this session
        /// </summary>
        private void Disconnect()
        {
            Background(new DisconnectAction(this));
        }

        private void View_Stop()
        {
            // Remove all pending actions)
            foreach (BackgroundAction action in getActions().toArray(
                new BackgroundAction[getActions().size()]))
            {
                action.cancel();
            }
            // Interrupt any pending operation by forcefully closing the socket
            Interrupt();
        }

        private void View_SendCustomCommand()
        {
            //todo implement
            throw new NotImplementedException();
        }

        private void View_Search()
        {
            View.StartSearch();
        }

        private void View_FolderInside()
        {
            Path selected = SelectedPath;
            if (null == selected)
            {
                return;
            }
            if (selected.attributes().isDirectory())
            {
                SetWorkdir(selected);
            }
            else if (selected.attributes().isFile() || View.SelectedPaths.Count > 1)
            {
                if (Preferences.instance().getBoolean("browser.doubleclick.edit"))
                {
                    View_EditEvent(null);
                }
                else
                {
                    View_Download();
                }
            }
        }

        /// <summary>
        /// Download to default download directory.
        /// </summary>
        /// <param name="downloads"></param>
        public void Download(IList<Path> downloads)
        {
            Download(downloads, _session.getHost().getDownloadFolder());
        }

        public void Download(IList<Path> downloads, Local downloadFolder)
        {
            Session session = getTransferSession();

            /*if (session == null) System.Windows.Forms.MessageBox.Show("SESSION IS NULL");
            else
            {
                System.Windows.Forms.MessageBox.Show("SESSION IS NOT NULL");
            
            }*/
            List roots = new Collection();
            foreach (Path selected in downloads)
            {
                Path path = PathFactory.createPath(session, selected.getAsDictionary());
                path.setLocal(LocalFactory.createLocal(downloadFolder, path.getName()));
                roots.add(path);
            }
            //System.Windows.Forms.MessageBox.Show("DOWNLOAD TRANSFER");
           
            Transfer q = new DownloadTransfer(roots);
            transfer(q);
        }

        private void View_GotoFolder()
        {
            GotoController gc = new GotoController(ObjectFactory.GetInstance<IGotoPromptView>(), this);
            gc.Show();
        }

        private void View_RefreshBrowser()
        {
            if (IsMounted())
            {
                Session session = getSession();
                session.cdn().clear();
                session.cache().invalidate(Workdir.getReference());
                foreach (Path path in View.VisiblePaths)
                {
                    if (null == path) continue;
                    session.cache().invalidate(path.getReference());
                }
                ReloadData(SelectedPaths);
            }
        }

        private bool View_ValidateTextEncoding()
        {
            return IsMounted() && !IsActivityRunning();
        }

        private void View_ToggleLogDrawer()
        {
            View.LogDrawerVisible = !View.LogDrawerVisible;
            Preferences.instance().setProperty("browser.logDrawer.isOpen", View.LogDrawerVisible);
        }

        private void View_ShowHiddenFiles()
        {
            ShowHiddenFiles = !ShowHiddenFiles;
            if (IsMounted())
            {
                ReloadData(true);
            }
        }

        private void View_ToggleToolbar()
        {
            View.ToolbarVisible = !View.ToolbarVisible;
            Preferences.instance().setProperty("browser.toolbar", View.ToolbarVisible);
        }

        private bool View_ValidatePaste()
        {
            //todo implement
            return false;
        }

        private void View_Paste()
        {
            //todo implement
            throw new NotImplementedException();
        }

        private bool View_ValidateCopy()
        {
            return false;
            return IsMounted() && SelectedPaths.Count > 0;
        }

        private void View_Copy()
        {
            //todo implement
            throw new NotImplementedException();
        }

        private bool View_ValidateCut()
        {
            return false;
            return IsMounted() && SelectedPaths.Count > 0;
        }

        private void View_Cut()
        {
            //todo implement
            return;
            throw new NotImplementedException();
        }

        private void View_ShowPreferences()
        {
            PreferencesController.Instance.View.Show();
        }

        private bool View_ContextMenuEnabled()
        {
            //context menu is always enabled
            return true;
        }

        private void View_Exit()
        {
            MainController.Exit();
        }

        private List<string> View_GetArchives()
        {
            List<string> result = new List<string>();
            Archive[] archives = Archive.getKnownArchives();
            foreach (Archive archive in archives)
            {
                List selected = Utils.ConvertToJavaList(SelectedPaths, null);
                result.Add(archive.getTitle(selected));
            }
            return result;
        }

        private bool View_ValidateExpandArchive()
        {
            if (IsMounted())
            {
                if (!getSession().isUnarchiveSupported())
                {
                    return false;
                }
                if (SelectedPaths.Count > 0)
                {
                    foreach (Path selected in SelectedPaths)
                    {
                        if (selected.attributes().isDirectory())
                        {
                            return false;
                        }
                        if (!Archive.isArchive(selected.getName()))
                        {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        private void View_ExpandArchive()
        {
            List<Path> expanded = new List<Path>();
            foreach (Path selected in SelectedPaths)
            {
                Archive archive = Archive.forName(selected.getName());
                if (null == archive)
                {
                    continue;
                }
                if (CheckOverwrite(Utils.ConvertFromJavaList<Path>(archive.getExpanded(new ArrayList {selected}))))
                {
                    background(new UnarchiveAction(this, archive, selected, expanded));
                }
            }
        }

        private bool View_ValidateCreateArchive()
        {
            if (IsMounted())
            {
                if (!getSession().isArchiveSupported())
                {
                    return false;
                }
                if (SelectedPaths.Count > 0)
                {
                    foreach (Path selected in SelectedPaths)
                    {
                        if (selected.attributes().isFile() && Archive.isArchive(selected.getName()))
                        {
                            // At least one file selected is already an archive. No distinct action possible
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        private void View_CreateArchive(object sender, CreateArchiveEventArgs createArchiveEventArgs)
        {
            Archive archive = Archive.forName(createArchiveEventArgs.ArchiveName);
            IList<Path> selected = SelectedPaths;
            if (CheckOverwrite(new List<Path> {archive.getArchive(Utils.ConvertToJavaList(selected))}))
            {
                background(new CreateArchiveAction(this, archive, selected));
            }
        }

        private bool View_ValidateDelete()
        {
            return IsMounted() && SelectedPaths.Count > 0;
        }

        private bool View_ValidateSynchronize()
        {
            return IsMounted();
        }

        private void View_Synchronize()
        {
            Path selection;
            if (SelectedPaths.Count == 1 && SelectedPath.attributes().isDirectory())
            {
                selection = SelectedPath;
            }
            else
            {
                selection = Workdir;
            }
            string folder = View.SynchronizeDialog(String.Format(Locale.localizedString("Synchronize {0} with"),
                                                                 selection.getName()), Environment.SpecialFolder.Desktop,
                                                   null);
            if (null != folder)
            {
                Path root = PathFactory.createPath(getTransferSession(true), selection.getAsDictionary());
                root.setLocal(LocalFactory.createLocal(folder));
                transfer(new SyncTransfer(root));
            }
        }

        private bool View_ValidateUpload()
        {
            return IsMounted();
        }

        private void View_Upload() //andre
        {
            // Due to the limited functionality of the OpenFileDialog class it is
            // currently not possible to select a folder. May be we should provide
            // a second menu item which allows to select a folder to upload
            string[] paths = View.UploadDialog(null);
            if (null == paths) return;

            Path destination = SelectedPath;
            if (null == destination)
            {
                destination = Workdir;
            }
            else if (!destination.attributes().isDirectory())
            {
                destination = destination.getParent();
            }
            Session session = getTransferSession();
            List roots = Utils.ConvertToJavaList(paths, path => PathFactory.createPath(session,
                                                                                       destination.getAbsolute(),
                                                                                       LocalFactory.createLocal(path)));
            Transfer q = new UploadTransfer(roots);
            transfer(q);
        }

        private void View_DownloadTo()
        {
            string folderName = View.DownloadToDialog(Locale.localizedString("Download To…"),
                                                      Environment.SpecialFolder.Desktop, null);
            if (null != folderName)
            {
                Local downloadFolder = LocalFactory.createLocal(folderName);
                Download(SelectedPaths, downloadFolder);
            }
        }

        private bool View_ValidateDownloadAs()
        {
            return IsMounted() && SelectedPaths.Count == 1;
        }

        private void View_DownloadAs()
        {
            string fileName = View.DownloadAsDialog(null, SelectedPath.getDisplayName());
            if (null != fileName)
            {
                Download(new List<Path> {SelectedPath}, LocalFactory.createLocal(fileName));
            }
        }

        private bool View_ValidateDownload()
        {
            return IsMounted() && SelectedPaths.Count > 0;
        }

        private bool View_ValidateShowInspector()
        {
            return IsMounted() && SelectedPaths.Count > 0;
        }

        private bool View_ValidateOpenWebUrl()
        {
            return IsMounted();
        }

        private bool View_ValidateEditWith()
        {
            if (IsMounted() && SelectedPaths.Count > 0)
            {
                foreach (Path selected in SelectedPaths)
                {
                    if (!IsEditable(selected))
                    {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        /// <param name="selected"></param>
        /// <returns>True if the selected path is editable (not a directory)</returns>
        private bool IsEditable(Path selected)
        {
            if (getSession().getHost().getCredentials().isAnonymousLogin())
            {
                return false;
            }
            return selected.attributes().isFile();
        }

        private bool View_ValidateDuplicateFile()
        {
            return IsMounted() && SelectedPaths.Count == 1;
        }

        private bool View_ValidateRenameFile()
        {
            if (IsMounted() && SelectedPaths.Count == 1)
            {
                if (null == SelectedPath)
                {
                    return false;
                }
                return getSession().isRenameSupported(SelectedPath);
            }
            return false;
        }

        private bool View_ValidateNewFile()
        {
            return IsMounted();
        }

        private void View_NewDownload()
        {
            //todo implement
            return;
            throw new NotImplementedException();
        }

        private void View_OpenConnection()
        {
            ConnectionController c = ConnectionController.Instance(this);
            DialogResult result = c.View.ShowDialog(View);
            if (result == DialogResult.OK)
            {
                Mount(c.ConfiguredHost);
            }
        }

        private bool View_ValidateNewFolder()
        {
            return IsMounted() && _session.isCreateFolderSupported(Workdir);
        }

        private void View_DuplicateFile()
        {
            DuplicateFileController dc =
                new DuplicateFileController(ObjectFactory.GetInstance<IDuplicateFilePromptView>(), this);
            dc.Show();
        }

        private void View_NewFile()
        {
            CreateFileController fc = new CreateFileController(ObjectFactory.GetInstance<ICreateFilePromptView>(), this);
            fc.Show();
        }

        private void View_Delete()
        {
            DeletePaths(SelectedPaths);
        }

        private void View_NewFolder()
        {
            FolderController fc = new FolderController(ObjectFactory.GetInstance<INewFolderPromptView>(), this);
            fc.Show();
        }

        private bool View_RenameFile(Path path, string newName)
        {
            if (!String.IsNullOrEmpty(newName) && !newName.Equals(path.getName()))
            {
                Path renamed = PathFactory.createPath(getSession(), path.getParent().getAbsolute(), newName,
                                                      path.attributes().getType());
                RenamePath(path, renamed);
            }
            return false;
        }

        private DataObject View_BrowserDrag(ObjectListView listView)
        {
            DataObject data = new DataObject(DataFormats.FileDrop, new[]
                {
                    CreateAndWatchTemporaryFile(
                        delegate(object sender,
                                 FileSystemEventArgs args)
                            {
                                dropFolder =
                                    System.IO.Path.
                                           GetDirectoryName(
                                               args.FullPath);
                                Invoke(
                                    delegate
                                        {
                                            Download(SelectedPaths,
                                                     LocalFactory.
                                                         createLocal(
                                                             dropFolder));
                                        });
                            }
                                                                       )
                });
            return data;
        }

        private void RemoveTemporaryFilesystemWatcher()
        {
            BeginInvoke(delegate
                {
                    foreach (FileSystemWatcher watcher in _temporaryWatcher)
                    {
                        watcher.Dispose();
                    }
                    _temporaryWatcher.Clear();
                });
        }

        private void RemoveTemporaryFiles(DataObject data)
        {
            if (data.ContainsFileDropList())
            {
                foreach (string tmpFile in data.GetFileDropList())
                {
                    if (File.Exists(tmpFile))
                    {
                        File.Delete(tmpFile);
                    }
                    if (null != dropFolder)
                    {
                        string tmpDestFile = System.IO.Path.Combine(dropFolder, System.IO.Path.GetFileName(tmpFile));
                        if (File.Exists(tmpDestFile))
                        {
                            File.Delete(tmpDestFile);
                        }
                    }
                }
            }
        }

        private void View_BrowserEndDrag(DataObject data)
        {
            RemoveTemporaryFiles(data);
            RemoveTemporaryFilesystemWatcher();
        }

        private void View_BrowserDropped(OlvDropEventArgs e)
        {
            if (IsMounted() && e.DataObject is DataObject && ((DataObject) e.DataObject).ContainsFileDropList())
            {
                Path destination;
                switch (e.DropTargetLocation)
                {
                    case DropTargetLocation.Item:
                        destination = (Path) e.DropTargetItem.RowObject;
                        break;
                    case DropTargetLocation.Background:
                        destination = Workdir;
                        break;
                    default:
                        destination = null;
                        break;
                }

                StringCollection dropList = (e.DataObject as DataObject).GetFileDropList();
                if (dropList.Count > 0)
                {
                    IList<Path> roots = new List<Path>();
                    Session session = getTransferSession();
                    foreach (string file in dropList)
                    {
                        Path p = PathFactory.createPath(session, destination.getAbsolute(),
                                                        LocalFactory.createLocal(file));
                        roots.Add(p);
                    }
                    UploadDroppedPath(roots, destination);
                }
            }
        }

        public void UploadDroppedPath(IList<Path> roots, Path destination)
        {
            if (IsMounted())
            {
                UploadTransfer q = new UploadTransfer(Utils.ConvertToJavaList(roots));
                if (q.numberOfRoots() > 0)
                {
                    transfer(q);
                }
            }
        }

        /// <summary>
        /// Check if we accept drag operation from an external program
        /// </summary>
        /// <param name="args"></param>
        private void View_BrowserCanDrop(OlvDropEventArgs args)
        {
            Log.trace("Entering View_BrowserCanDrop with " + args.Effect);
            if (IsMounted() && !(args.DataObject is OLVDataObject))
            {
                if (args.DataObject is DataObject && ((DataObject) args.DataObject).ContainsFileDropList())
                {
                    Path destination;
                    switch (args.DropTargetLocation)
                    {
                        case DropTargetLocation.Item:
                            destination = (Path) args.DropTargetItem.RowObject;
                            if (!destination.attributes().isDirectory())
                            {
                                //dragging over file
                                destination = destination.getParent();
                            }
                            break;
                        case DropTargetLocation.Background:
                            destination = Workdir;
                            break;
                        default:
                            args.Effect = DragDropEffects.None;
                            args.DropTargetLocation = DropTargetLocation.None;
                            return;
                    }
                    if (!getSession().isCreateFileSupported(destination))
                    {
                        Log.trace("Session does not allow file creation");
                        args.Effect = DragDropEffects.None;
                        args.DropTargetLocation = DropTargetLocation.None;
                        return;
                    }
                    Log.trace("Setting effect to copy");
                    args.Effect = DragDropEffects.Copy;
                    if (Workdir == destination)
                    {
                        args.DropTargetLocation = DropTargetLocation.Background;
                    }
                    else
                    {
                        args.DropTargetItem = args.ListView.ModelToItem(destination);
                    }
                    (args.DataObject as DataObject).SetDropDescription((DropImageType) args.Effect, "Copy to %1",
                                                                       destination.getName());
                }
            }
        }

        private void View_ShowTransfers()
        {
            TransferController.Instance.View.Show();
        }

        private void View_ShowInspector()
        {
            IList<Path> selected = SelectedPaths;
            if (selected.Count > 0)
            {
                if (Preferences.instance().getBoolean("browser.info.isInspector"))
                {
                    if (null == _inspector || _inspector.View.IsDisposed)
                    {
                        _inspector = InfoController.Factory.Create(this, selected);
                    }
                    else
                    {
                        _inspector.Files = selected;
                    }
                    _inspector.View.Show(View);
                }
                else
                {
                    InfoController c = InfoController.Factory.Create(this, selected);
                    c.View.Show(View);
                }
            }
        }

        private void View_EditEvent(string exe)
        {
            foreach (Path selected in SelectedPaths)
            {
                Editor editor;
                if (Utils.IsBlank(exe))
                {
                    editor = EditorFactory.instance().create(this, selected);
                }
                else
                {
                    editor = EditorFactory.instance().create(this, new Application(exe, null), selected);
                }
                editor.open();
            }
        }

        private void UpdateEditIcon()
        {
            Path selected = SelectedPath;
            if (null != selected)
            {
                if (IsEditable(selected))
                {
                    Application app = EditorFactory.instance().getEditor(selected.getName());
                    string editCommand = app != null ? app.getIdentifier() : null;
                    if (Utils.IsNotBlank(editCommand))
                    {
                        View.EditIcon =
                            IconCache.Instance.GetFileIconFromExecutable(editCommand, IconCache.IconSize.Large).ToBitmap
                                ();
                        return;
                    }
                }
            }
            View.EditIcon = IconCache.Instance.IconForName("pencil", 32);
        }

        private void UpdateOpenIcon()
        {
            View.OpenIcon = IconCache.Instance.GetDefaultBrowserIcon();
        }

        private void View_BrowserSelectionChanged()
        {
            UpdateEditIcon();

            // update inspector content if available
            IList<Path> selectedPaths = SelectedPaths;

            if (Preferences.instance().getBoolean("browser.info.isInspector"))
            {
                if (_inspector != null && _inspector.Visible)
                {
                    if (selectedPaths.Count > 0)
                    {
                        background(new UpdateInspectorAction(this, selectedPaths));
                    }
                }
            }
        }

        private void View_PathSelectionChanged()
        {
            string selected = View.SelectedComboboxPath;
            Path previous = Workdir;
            if (selected != null)
            {
                Path path = PathFactory.createPath(_session, selected, AbstractPath.DIRECTORY_TYPE);
                SetWorkdir(path);
                if (previous.getParent().equals(path))
                {
                    SetWorkdir(path, previous);
                }
                else
                {
                    SetWorkdir(path);
                }
            }
        }

        private void ViewViewShownEvent()
        {
            UpdateNavigationPaths();
        }

        private void View_FolderUp()
        {
            Path previous = Workdir;
            SetWorkdir(previous.getParent(), previous);
        }

        private void View_HistoryForward()
        {
            Path selected = GetForwardPath();
            if (selected != null)
            {
                SetWorkdir(selected);
            }
        }

        private void View_HistoryBack()
        {
            Path selected = GetPreviousPath();
            if (selected != null)
            {
                Path previous = Workdir;
                if (previous.getParent().equals(selected))
                {
                    SetWorkdir(selected, previous);
                }
                else
                {
                    SetWorkdir(selected);
                }
            }
        }

        private void View_BrowserDoubleClicked()
        {
            View_FolderInside();
        }

        private void View_QuickConnect()
        {
            if (string.IsNullOrEmpty(View.QuickConnectValue))
            {
                return;
            }
            string input = View.QuickConnectValue.Trim();

            // First look for equivalent bookmarks
            BookmarkCollection bookmarkCollection = BookmarkCollection.defaultCollection();
            foreach (Host host in bookmarkCollection)
            {
                if (host.getNickname().Equals(input))
                {
                    Mount(host);
                    return;
                }
            }
            Mount(Host.parse(input));
        }

        /// <summary>
        /// Open a new browser with the current selected folder as the working directory
        /// </summary>
        private void View_NewBrowser(object sender, NewBrowserEventArgs newBrowserEventArgs)
        {
            if (newBrowserEventArgs.SelectedAsWorkingDir)
            {
                Path selected = SelectedPath;
                if (null == selected || !selected.attributes().isDirectory())
                {
                    selected = Workdir;
                }
                BrowserController c = MainController.NewBrowser(true);

                Host host = new Host(getSession().getHost().getAsDictionary());
                host.setDefaultPath(selected.getAbsolute());
                c.Mount(host);
            }
            else
            {
                BrowserController c = MainController.NewBrowser(true);
                MainController.OpenDefaultBookmark(c);
            }
        }

        public Session getSession()
        {
            return _session;
        }

        /// <summary>
        /// Transfers the files either using the queue or using
        /// the browser session if #connection.pool.max is 1
        /// </summary>
        /// <param name="transfer"></param>
        public void transfer(Transfer transfer)
        {
            //System.Windows.Forms.MessageBox.Show("iniciando transfer");
           
            this.transfer(transfer, getSession().getMaxConnections() == 1);
       
        }

        /// <summary>
        /// Will reload the data for this directory in the browser after the transfer completes
        /// </summary>
        /// <param name="transfer"></param>
        /// <param name="destination"></param>
        public void transfer(Transfer transfer, bool browser)
        {
           // System.Windows.Forms.MessageBox.Show("iniciando transfer 2");
            this.transfer(transfer, browser, new LazyTransferPrompt(this, transfer));
        }

        /// <summary>
        ///
        /// </summary>
        /// <param name="transfer"></param>
        /// <param name="destination"></param>
        /// <param name="useBrowserConnection"></param>
        public void transfer(Transfer transfer, List<Path> selected, bool browser)
        {
            this.transfer(transfer, selected, browser, new LazyTransferPrompt(this, transfer));
        }

        /// <summary>
        /// 
        /// </summary>
        /// <param name="transfer"></param>
        /// <param name="destination"></param>
        /// <param name="useBrowserConnection"></param>
        /// <param name="prompt"></param>
        public void transfer(Transfer transfer, bool browser, TransferPrompt prompt)
        {
            //System.Windows.Forms.MessageBox.Show("iniciando transfer 3");
            this.transfer(transfer, Utils.ConvertFromJavaList<Path>(transfer.getRoots(), null), browser, prompt);
        }

        public void transfer(Transfer transfer, IList<Path> changed, bool browser, TransferPrompt prompt)
        {
            //System.Windows.Forms.MessageBox.Show("iniciando transfer 4 ");
            transfer.addListener(new ReloadTransferAdapter(this, transfer, changed));
            if (browser)
            {
              //  System.Windows.Forms.MessageBox.Show("oldbrowser");
                transfer.addListener(new ProgressTransferAdapter(this, transfer));
                Background(new TransferBrowserBackgroundAction(this, prompt, transfer));
            }
            else
            {
                // in new browser
               // System.Windows.Forms.MessageBox.Show("novo browser");//andre
                TransferController.Instance.StartTransfer(transfer);
            }
        }

        /// <summary>
        ///
        /// </summary>
        /// <returns>The session to be used for file transfers. Null if not mounted</returns>
        public Session getTransferSession()
        {
          //  System.Windows.Forms.MessageBox.Show("get session");//andre
          
            return getTransferSession(false);
        }

        public Session getTransferSession(bool force)
        {
            if (!IsMounted())
            {
                return null;
            }
            if (!force)
            {
                if (_session.getMaxConnections() == 1)
                {
                    //System.Windows.Forms.MessageBox.Show("sessao existente");//

          
                    return _session;
                }
            }
            //System.Windows.Forms.MessageBox.Show("problema encontrado");//



          
            return SessionFactory.createSession(_session.getHost());
        }

        /// <summary>
        ///
        /// </summary>
        /// <returns>true if a connection is being opened or is already initialized</returns>
        public bool HasSession()
        {
            return _session != null;
        }

        public bool IsMounted()
        {
            return HasSession() && Workdir != null;
        }

        /// <summary>
        ///
        /// </summary>
        /// <param name="preserveSelection">All selected files should be reselected after reloading the view</param>
        public void ReloadData(bool preserveSelection)
        {
            if (preserveSelection)
            {
                //Remember the previously selected paths
                ReloadData(SelectedPaths);
            }
            else
            {
                ReloadData(new List<Path>());
            }
        }

        public void RefreshParentPath(Path changed)
        {
            RefreshParentPaths(new Collection<Path> {changed});
        }

        public void RefreshParentPaths(IList<Path> changed)
        {
            RefreshParentPaths(changed, new List<Path>());
        }

        public void RefreshParentPaths(IList<Path> changed, IList<Path> selected)
        {
            bool rootRefreshed = false; //prevent multiple root updates
            foreach (Path path in changed)
            {
                getSession().cache().invalidate(path.getParent().getReference());
                Path lookup = getSession().cache().lookup(path.getParent().getReference());
                if (null == lookup || Workdir.equals(lookup))
                {
                    if (rootRefreshed)
                    {
                        continue;
                    }
                    View.SetBrowserModel(_browserModel.ChildrenGetter(Workdir));
                    rootRefreshed = true;
                }
                else
                {
                    View.RefreshBrowserObject(lookup);
                }
            }
            SelectedPaths = selected;
        }

        protected void ReloadData(IList<Path> selected)
        {
            if (null != Workdir)
            {
                IEnumerable<Path> children = _browserModel.ChildrenGetter(Workdir);
                //clear selection before resetting model. Otherwise we have weird selection effects.
                SelectedPaths = new List<Path>();
                View.SetBrowserModel(children);
                SelectedPaths = selected;
                List<Path> toUpdate = new List<Path>();
                foreach (Path path in View.VisiblePaths)
                {
                    if (path.attributes().isDirectory())
                    {
                        toUpdate.Add(path);
                    }
                }
                View.RefreshBrowserObjects(toUpdate);
            }
            else
            {
                View.SetBrowserModel(null);
            }
            View.FilenameFilter = FilenameFilter;
            UpdateStatusLabel();
        }

        public void SetWorkdir(Path directory)
        {
            SetWorkdir(directory, new List<Path>());
        }

        public void SetWorkdir(Path directory, Path selected)
        {
            SetWorkdir(directory, new List<Path> {selected});
        }

        /// <summary>
        /// Sets the current working directory. This will udpate the path selection dropdown button
        /// and also add this path to the browsing history. If the path cannot be a working directory (e.g. permission
        /// issues trying to enter the directory), reloading the browser view is canceled and the working directory
        /// not changed.
        /// </summary>
        /// <param name="directory">The new working directory to display or null to detach any working directory from the browser</param>
        /// <param name="selected"></param>
        public void SetWorkdir(Path directory, List<Path> selected)
        {
            if (null == directory)
            {
                // Clear the browser view if no working directory is given
                Invoke(delegate
                    {
                        Workdir = null;
                        UpdateNavigationPaths();
                        ReloadData(false);
                    });
                return;
            }
            Background(new WorkdirAction(this, directory, selected));
        }

        private void UpdateNavigationPaths()
        {
            List<string> paths = new List<string>();
            if (!IsMounted())
            {
                View.PopulatePaths(new List<string>());
            }
            else
            {
                Path p = Workdir;
                while (!p.getParent().equals(p))
                {
                    paths.Add(p.getAbsolute());
                    p = p.getParent();
                }
                paths.Add(p.getAbsolute());
                View.PopulatePaths(paths);
            }
        }

        public void RefreshObject(Path path, bool preserveSelection)
        {
            if (preserveSelection)
            {
                RefreshObject(path, View.SelectedPaths);
            }
            else
            {
                RefreshObject(path, new List<Path>());
            }
        }

        public void RefreshObject(Path path, IList<Path> selected)
        {
            if (Workdir.Equals(path))
            {
                View.SetBrowserModel(_browserModel.ChildrenGetter(path));
            }
            else
            {
                if (!path.attributes().isDirectory())
                {
                    View.RefreshBrowserObject(path.getParent());
                }
                else
                {
                    View.RefreshBrowserObject(path);
                }
            }
            SelectedPaths = selected;
            UpdateStatusLabel();
        }

        public void Mount(Host host)
        {
            CallbackDelegate callbackDelegate = delegate
                {
                    // The browser has no session, we are allowed to proceed
                    // Initialize the browser with the new session attaching all listeners
                    Session session = Init(host);
                    if(session != null)
                    background(new MountAction(this, session, host));
                };
            Unmount(callbackDelegate);
        }

        /// <summary>
        /// Initializes a session for the passed host. Setting up the listeners and adding any callback
        /// controllers needed for login, trust management and hostkey verification.
        /// </summary>
        /// <param name="host"></param>
        /// <returns>A session object bound to this browser controller</returns>
        private Session Init(Host host)
        {
            if (HasSession())
            {
                _session.removeProgressListener(_progress);
                _session.removeConnectionListener(_listener);
            }
            
            _session = SessionFactory.createSession(host);

            /*
            //Checa protocolo
            if (String.Compare(host.getProtocol().getIdentifier(), "swiftkeystonefederated") == 0) {
                //pega lista de idps
                java.util.List idPList = _session.getIdps();
                //
                //Exibindo Dialog
                ui.winforms.SelectIdp dlgSelectIdP = new ui.winforms.SelectIdp(idPList);
                dlgSelectIdP.StartPosition = FormStartPosition.CenterParent;
                DialogResult resultfederado = dlgSelectIdP.ShowDialog();
                if (resultfederado != DialogResult.OK)
                {
                    return null;    
                }

                System.Windows.Forms.MessageBox.Show(dlgSelectIdP.IdPServer);   
            }*/
            SetWorkdir(null);
            View.SelectedEncoding = _session.getEncoding();
            _session.addProgressListener(_progress = new ProgessListener(this));
            _session.addConnectionListener(_listener = new ConnectionAdapter(this, host));
            View.ClearTranscript();
            ClearBackHistory();
            ClearForwardHistory();
            _session.addTranscriptListener(this);
            return _session;
        }

        /// <summary>
        /// Remove all entries from the back path history
        /// </summary>
        public void ClearBackHistory()
        {
            _backHistory.Clear();
        }

        /// <summary>
        /// Remove all entries from the forward path history
        /// </summary>
        public void ClearForwardHistory()
        {
            _forwardHistory.Clear();
        }

        // some simple caching as _session.isConnected() throws a ConnectionCanceledException if not connected

        /// <summary>
        ///
        /// </summary>
        /// <returns>true if mounted and the connection to the server is alive</returns>
        public bool IsConnected()
        {
            if (IsMounted())
            {
                if (_sessionShouldBeConnected)
                {
                    return _session.isConnected();
                }
            }
            return false;
        }

        /// <summary>
        ///
        /// </summary>
        /// <returns>true if there is any network activity running in the background</returns>
        public bool IsActivityRunning()
        {
            BackgroundAction current = getActions().getCurrent();
            if (null == current)
            {
                return false;
            }
            if (current is BrowserBackgroundAction)
            {
                return ((BrowserBackgroundAction) current).BrowserController == this;
            }
            return false;
        }

        public static bool ApplicationShouldTerminate()
        {
            // Determine if there are any open connections
            foreach (BrowserController controller in new List<BrowserController>(MainController.Browsers))
            {
                BrowserController c = controller;
                if (!controller.Unmount(delegate(DialogResult result)
                    {
                        if (DialogResult.OK == result)
                        {
                            c.View.Dispose();
                            return true;
                        }
                        return false;
                    }, delegate { }))
                {
                    return false; // Disconnect cancelled
                }
            }
            return true;
        }

        public bool Unmount()
        {
            return Unmount(() => { });
        }

        public bool Unmount(CallbackDelegate disconnected)
        {
            return Unmount(result =>
                {
                    if (DialogResult.OK == result)
                    {
                        UnmountImpl(disconnected);
                        return true;
                    }
                    // No unmount yet
                    return false;
                }, disconnected);
        }

        /// <summary>
        ///
        /// </summary>
        /// <param name="unmountImpl"></param>
        /// <param name="disconnected"></param>
        /// <returns>True if the unmount process is in progress or has been finished, false if cancelled</returns>
        public bool Unmount(DialogCallbackDelegate unmountImpl, CallbackDelegate disconnected)
        {
            if (IsConnected() || IsActivityRunning())
            {
                if (Preferences.instance().getBoolean("browser.confirmDisconnect"))
                {
                    DialogResult result = CommandBox(Locale.localizedString("Disconnect"),
                                                     String.Format(Locale.localizedString("Disconnect from {0}"),
                                                                   _session.getHost().getHostname()),
                                                     Locale.localizedString("The connection will be closed."),
                                                     String.Format("{0}", Locale.localizedString("Disconnect")),
                                                     true,
                                                     Locale.localizedString("Don't ask again", "Configuration"),
                                                     SysIcons.Question, delegate(int option, bool verificationChecked)
                                                         {
                                                             if (verificationChecked)
                                                             {
                                                                 // Never show again.
                                                                 Preferences.instance().setProperty(
                                                                     "browser.confirmDisconnect",
                                                                     false);
                                                             }
                                                             switch (option)
                                                             {
                                                                 case 0: // Disconnect
                                                                     unmountImpl(DialogResult.OK);
                                                                     break;
                                                             }
                                                         });
                    return DialogResult.OK == result;
                }
                UnmountImpl(disconnected);
                // Unmount in progress
                return true;
            }
            disconnected();
            // Unmount succeeded
            return true;
        }

        private void UnmountImpl(CallbackDelegate disconnected)
        {
            if (IsActivityRunning())
            {
                Interrupt();
            }
            background(new UnmountAction(this, disconnected));
        }

        private void Interrupt()
        {
            if (HasSession())
            {
                if (IsActivityRunning())
                {
                    BackgroundAction current = getActions().getCurrent();
                    if (null != current)
                    {
                        current.cancel();
                    }
                }
                background(new InterruptAction(this, _session));
            }
        }


        /// <summary>
        /// Will close the session but still display the current working directory without any confirmation
        /// from the user
        /// </summary>
        private void UnmountImpl()
        {
            // This is not synchronized to the <code>mountingLock</code> intentionally; this allows to unmount
            // sessions not yet connected
            if (HasSession())
            {
                //Close the connection gracefully
                _session.close();
            }
        }

        public void UpdateStatusLabel()
        {
            string label = Locale.localizedString("Disconnected", "Status");

            switch (View.CurrentView)
            {
                case BrowserView.File:
                    BackgroundAction current = getActions().getCurrent();
                    if (null == current)
                    {
                        if (IsConnected())
                        {
                            label = String.Format(Locale.localizedString("{0} Files"), View.NumberOfFiles);
                        }
                    }
                    else
                    {
                        if (Utils.IsNotBlank(_progress.Laststatus))
                        {
                            label = _progress.Laststatus;
                        }
                        else
                        {
                            label = current.getActivity();
                        }
                    }
                    break;
                case BrowserView.Bookmark:
                case BrowserView.History:
                case BrowserView.Bonjour:
                    label = View.NumberOfBookmarks + " " + Locale.localizedString("Bookmarks");
                    break;
            }
            UpdateStatusLabel(label);
        }

        public void UpdateStatusLabel(string label)
        {
            View.StatusLabel = label;
        }

        public void AddPathToHistory(Path path)
        {
            if (_backHistory.Count > 0)
            {
                // Do not add if this was a reload
                if (path.equals(_backHistory.Contains(_backHistory[_backHistory.Count - 1])))
                {
                    return;
                }
            }
            _backHistory.Add(path);
        }

        /// <summary>
        /// Returns the prevously browsed path and moves it to the forward history
        /// </summary>
        /// <returns>The previously browsed path or null if there is none</returns>
        public Path GetPreviousPath()
        {
            int size = _backHistory.Count;
            if (size > 1)
            {
                _forwardHistory.Add(_backHistory[size - 1]);
                Path p = _backHistory[size - 2];
                //delete the fetched path - otherwise we produce a loop
                _backHistory.RemoveAt(size - 1);
                _backHistory.RemoveAt(size - 2);
                return p;
            }
            if (1 == size)
            {
                _forwardHistory.Add(_backHistory[size - 1]);
                return _backHistory[size - 1];
            }
            return null;
        }

        /// <summary>
        ///
        /// </summary>
        /// <returns>The last path browsed before #getPreviousPath was called</returns>
        public Path GetForwardPath()
        {
            int size = _forwardHistory.Count;
            if (size > 0)
            {
                Path path = _forwardHistory[size - 1];
                _forwardHistory.RemoveAt(size - 1);
                return path;
            }
            return null;
        }

        private void PopulateQuickConnect()
        {
            List<string> nicknames = new List<string>();
            foreach (Host host in _bookmarkCollection)
            {
                nicknames.Add(host.getNickname());
            }
            View.PopulateQuickConnect(nicknames);
        }

        /// <summary>
        ///
        /// </summary>
        /// <param name="path">The existing file</param>
        /// <param name="renamed">The renamed file</param>
        protected internal void RenamePath(Path path, Path renamed)
        {
            RenamePaths(new Dictionary<Path, Path> {{path, renamed}});
        }

        /// <summary>
        ///
        /// </summary>
        /// <param name="selected">
        /// A dictionary with the original files as the key and
        /// the destination files as the value
        /// </param>
        protected internal void RenamePaths(IDictionary<Path, Path> selected)
        {
            if (CheckMove(selected.Values))
            {
                MoveTransfer move = new MoveTransfer(Utils.ConvertToJavaMap(selected));
                List<Path> changed = new List<Path>();
                changed.AddRange(selected.Keys);
                changed.AddRange(selected.Values);
                transfer(move, changed, true);
            }
        }

        /// <summary>
        /// Displays a warning dialog about files to be moved
        /// </summary>
        /// <param name="selected">The files to check for existance</param>
        /// <param name="action"></param>
        private bool CheckMove(ICollection<Path> selected)
        {
            if (selected.Count > 0)
            {
                if (Preferences.instance().getBoolean("browser.confirmMove"))
                {
                    StringBuilder alertText = new StringBuilder(
                        Locale.localizedString("Do you want to move the selected files?"));

                    StringBuilder content = new StringBuilder();
                    int i = 0;
                    IEnumerator<Path> enumerator = null;
                    for (enumerator = selected.GetEnumerator(); i < 10 && enumerator.MoveNext();)
                    {
                        Path item = enumerator.Current;
                        // u2022 = Bullet
                        content.Append("\n" + Character.toString('\u2022') + " " + item.getName());
                        i++;
                    }
                    if (enumerator.MoveNext())
                    {
                        content.Append("\n" + Character.toString('\u2022') + " ...)");
                    }
                    DialogResult r = QuestionBox(Locale.localizedString("Move"),
                                                 alertText.ToString(),
                                                 content.ToString(),
                                                 String.Format("{0}", Locale.localizedString("Move")),
                                                 true);
                    if (r == DialogResult.OK)
                    {
                        return CheckOverwrite(selected);
                    }
                }
                else
                {
                    return CheckOverwrite(selected);
                }
            }
            return false;
        }

        /// <summary>
        /// Prunes the list of selected files. Files which are a child of an already included directory
        /// are removed from the returned list.
        /// </summary>
        /// <param name="selected"></param>
        /// <returns></returns>
        protected List<Path> CheckHierarchy(ICollection<Path> selected)
        {
            List<Path> normalized = new List<Path>();
            foreach (Path f in selected)
            {
                bool duplicate = false;
                foreach (Path n in normalized)
                {
                    if (f.isChild(n))
                    {
                        // The selected file is a child of a directory
                        // already included for deletion
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate)
                {
                    normalized.Add(f);
                }
            }
            return normalized;
        }

        /// <summary>
        /// Recursively deletes the files
        /// </summary>
        /// <param name="selected">The files selected in the browser to delete</param>
        public void DeletePaths(ICollection<Path> selected)
        {
            List<Path> normalized = CheckHierarchy(selected);
            if (normalized.Count == 0)
            {
                return;
            }

            StringBuilder alertText = new StringBuilder(
                Locale.localizedString(
                    "Really delete the following files? This cannot be undone."));

            StringBuilder content = new StringBuilder();
            int i = 0;
            IEnumerator<Path> enumerator;
            for (enumerator = selected.GetEnumerator(); i < 10 && enumerator.MoveNext();)
            {
                Path item = enumerator.Current;
                if (item.exists())
                {
                    if (i > 0) content.AppendLine();
                    // u2022 = Bullet
                    content.Append(Character.toString('\u2022') + " " + item.getName());
                }
                i++;
            }
            if (enumerator.MoveNext())
            {
                content.Append("\n" + Character.toString('\u2022') + " ...)");
            }
            DialogResult r = QuestionBox(Locale.localizedString("Delete"),
                                         alertText.ToString(),
                                         content.ToString(),
                                         String.Format("{0}", Locale.localizedString("Delete")),
                                         true);
            if (r == DialogResult.OK)
            {
                DeletePathsImpl(normalized);
            }
        }

        private void DeletePathsImpl(List<Path> files)
        {
            background(new DeleteAction(this, files));
        }

        public void SetPathFilter(string searchString)
        {
            Log.debug("setPathFilter:" + searchString);
            if (Utils.IsBlank(searchString))
            {
                View.SearchString = String.Empty;
                // Revert to the last used default filter
                if (ShowHiddenFiles)
                {
                    FilenameFilter = new NullPathFilter();
                }
                else
                {
                    FilenameFilter = new HiddenFilesPathFilter();
                }
            }
            else
            {
                // Setting up a custom filter for the directory listing
                FilenameFilter = new CustomPathFilter(searchString, this);
            }
            ReloadData(true);
        }

        /// <summary>
        /// Displays a warning dialog about already existing files
        /// </summary>
        /// <param name="selected">The files to check for existance</param>
        private bool CheckOverwrite(ICollection<Path> selected)
        {
            if (selected.Count > 0)
            {
                StringBuilder alertText = new StringBuilder(
                    Locale.localizedString(
                        "A file with the same name already exists. Do you want to replace the existing file?"));

                StringBuilder content = new StringBuilder();
                int i = 0;
                IEnumerator<Path> enumerator = null;
                bool shouldWarn = false;
                for (enumerator = selected.GetEnumerator(); enumerator.MoveNext();)
                {
                    Path item = enumerator.Current;
                    if (item.exists())
                    {
                        if (i < 10)
                        {
                            // u2022 = Bullet
                            content.Append("\n" + Character.toString('\u2022') + " " + item.getName());
                        }
                        shouldWarn = true;
                    }
                    i++;
                }
                if (i >= 10)
                {
                    content.Append("\n" + Character.toString('\u2022') + " ...)");
                }
                if (shouldWarn)
                {
                    DialogResult r = QuestionBox(Locale.localizedString("Overwrite"),
                                                 alertText.ToString(),
                                                 content.ToString(),
                                                 String.Format("{0}", Locale.localizedString("Overwrite")),
                                                 true);
                    if (r == DialogResult.OK)
                    {
                        return true;
                    }
                }
                else
                {
                    return true;
                }
            }
            return false;
        }

        /// <summary>
        ///
        /// </summary>
        /// <param name="source">The original file to duplicate</param>
        /// <param name="destination">The destination of the duplicated file</param>
        protected internal void DuplicatePath(Path source, Path destination)
        {
            DuplicatePaths(new Dictionary<Path, Path> {{source, destination}}, true);
        }

        /// <summary>
        ///
        /// </summary>
        /// <param name="selected">A dictionary with the original files as the key and the destination files as the value</param>
        ///<param name="browser"></param>
        protected internal void DuplicatePaths(IDictionary<Path, Path> selected, bool browser)
        {
            if (CheckOverwrite(selected.Values))
            {
                CopyTransfer copy = new CopyTransfer(Utils.ConvertToJavaMap(selected));
                List<Path> changed = new List<Path>();
                changed.AddRange(selected.Values);
                transfer(copy, changed, browser);
            }
        }

        /// <summary>
        ///
        /// </summary>
        /// <param name="view">The view to show</param>
        public void ToggleView(BrowserView view)
        {
            Log.debug("ToggleView:" + view);
            if (View.CurrentView == view) return;

            SetBookmarkFilter(null);
            switch (view)
            {
                case BrowserView.File:
                    View.CurrentView = BrowserView.File;
                    UpdateStatusLabel();
                    //ReloadData(false); //not necessary?
                    break;
                case BrowserView.Bookmark:
                    View.CurrentView = BrowserView.Bookmark;
                    _bookmarkModel.Source = BookmarkCollection.defaultCollection();
                    ReloadBookmarks();
                    SelectHost();
                    break;
                case BrowserView.History:
                    View.CurrentView = BrowserView.History;
                    _bookmarkModel.Source = HistoryCollection.defaultCollection();
                    ReloadBookmarks();
                    SelectHost();
                    break;
                case BrowserView.Bonjour:
                    View.CurrentView = BrowserView.Bonjour;
                    _bookmarkModel.Source = RendezvousCollection.defaultCollection();
                    ReloadBookmarks();
                    SelectHost();
                    break;
            }
        }

        private void SelectHost()
        {
            if (IsMounted())
            {
                View.SelectBookmark(getSession().getHost());
            }
        }

        /// <summary>
        /// Reload bookmarks table from the currently selected model
        /// </summary>
        public void ReloadBookmarks()
        {
            ReloadBookmarks(null);
        }

        /// <summary>
        /// Reload bookmarks table from the currently selected model
        /// </summary>
        public void ReloadBookmarks(Host selected)
        {
            //Note: expensive for a big bookmark list (might need a refactoring)
            View.SetBookmarkModel(_bookmarkModel.Source, selected);
            UpdateStatusLabel();
        }

        private class BookmarkFilter : HostFilter
        {
            private readonly string _searchString;

            public BookmarkFilter(String searchString)
            {
                _searchString = searchString;
            }

            public bool accept(Host host)
            {
                return host.getNickname().ToLower().Contains(_searchString.ToLower())
                       ||
                       (null == host.getComment()
                            ? false
                            : host.getComment().ToLower().Contains(_searchString.ToLower()))
                       || host.getHostname().ToLower().Contains(_searchString.ToLower());
            }
        }

        internal class ConnectionAdapter : ConnectionListener
        {
            private readonly BrowserController _controller;
            private readonly Host _host;

            public ConnectionAdapter(BrowserController controller, Host host)
            {
                _controller = controller;
                _host = host;
            }

            public void connectionWillOpen()
            {
                _controller._sessionShouldBeConnected = true;
                AsyncDelegate mainAction = delegate
                    {
                        _controller.View.RefreshBookmark(_controller.getSession().getHost());
                        _controller.View.WindowTitle = _host.getNickname();
                    };
                _controller.Invoke(new SimpleWindowMainAction(mainAction, _controller));
            }

            public void connectionDidOpen()
            {
                AsyncDelegate mainAction = delegate
                    {
                        _controller.View.RefreshBookmark(_controller.getSession().getHost());
                        ch.cyberduck.ui.growl.Growl.instance().notify("Connection opened",
                                                                      _host.getHostname());

                        _controller.View.SecureConnection = _controller._session.isSecure();
                        _controller.View.CertBasedConnection =
                            _controller._session is SSLSession;
                        _controller.View.SecureConnectionVisible = true;
                    };
                _controller.Invoke(new SimpleWindowMainAction(mainAction, _controller));
            }

            public void connectionWillClose()
            {
            }

            public void connectionDidClose()
            {
                _controller._sessionShouldBeConnected = false;
                AsyncDelegate mainAction = delegate
                    {
                        _controller.View.RefreshBookmark(_controller.getSession().getHost());
                        if (!_controller.IsMounted())
                        {
                            _controller.View.WindowTitle =
                                Preferences.instance().getProperty(
                                    "application.name");
                        }
                        _controller.View.SecureConnectionVisible = false;
                        _controller.UpdateStatusLabel();
                    };
                _controller.Invoke(new SimpleWindowMainAction(mainAction, _controller));
            }
        }

        private class CreateArchiveAction : BrowserBackgroundAction
        {
            private readonly Archive _archive;
            private readonly IList<Path> _selected;
            private readonly List _selectedJava;

            public CreateArchiveAction(BrowserController controller, Archive archive, IList<Path> selected)
                : base(controller)
            {
                _archive = archive;
                _selectedJava = Utils.ConvertToJavaList(selected);
                _selected = selected;
            }

            public override void run()
            {
                BrowserController._session.archive(_archive, _selectedJava);
            }

            public override string getActivity()
            {
                return _archive.getCompressCommand(_selectedJava);
            }

            public override void cleanup()
            {
                BrowserController.RefreshParentPaths(_selected,
                                                     new List<Path>
                                                         {_archive.getArchive(_selectedJava)});
            }
        }

        private class CustomPathFilter : PathFilter, IModelFilter
        {
            private readonly BrowserController _controller;
            private readonly String _searchString;

            public CustomPathFilter(String searchString, BrowserController controller)
            {
                _searchString = searchString;
                _controller = controller;
            }

            public bool Filter(object modelObject)
            {
                return accept((Path) modelObject);
            }

            public bool accept(AbstractPath file)
            {
                if (file.getName().ToLower().IndexOf(_searchString.ToLower()) != -1)
                {
                    // Matching filename
                    return true;
                }
                if (file.attributes().isDirectory())
                {
                    // #471. Expanded item childs may match search string
                    return _controller.getSession().cache().isCached(file.getReference());
                }
                return false;
            }
        }

        private class DeleteAction : BrowserBackgroundAction
        {
            private readonly List<Path> _normalized;

            public DeleteAction(BrowserController controller, List<Path> normalized)
                : base(controller)
            {
                _normalized = normalized;
            }

            public override void run()
            {
                foreach (Path p in _normalized)
                {
                    if (isCanceled())
                    {
                        break;
                    }
                    p.delete();
                    if (!BrowserController.IsConnected())
                    {
                        break;
                    }
                }
            }

            public override string getActivity()
            {
                return String.Format(Locale.localizedString("Deleting {0}", "Status"), String.Empty);
            }

            public override void cleanup()
            {
                BrowserController.RefreshParentPaths(_normalized);
            }
        }

        private class DisconnectAction : BrowserBackgroundAction
        {
            public DisconnectAction(BrowserController controller)
                : base(controller)
            {
            }

            public override void run()
            {
                BrowserController.UnmountImpl();
            }

            public override void cleanup()
            {
                if (Preferences.instance().getBoolean("browser.disconnect.showBookmarks"))
                {
                    BrowserController.ToggleView(BrowserView.Bookmark);
                }
                else
                {
                    BrowserController.View.BrowserActiveStateChanged();
                }
            }

            public override string getActivity()
            {
                return String.Format(Locale.localizedString("Disconnecting {0}", "Status"),
                                     BrowserController.getSession().getHost().getHostname());
            }
        }

        private class EncodingBrowserBackgroundAction : BrowserBackgroundAction
        {
            private readonly string _encoding;

            public EncodingBrowserBackgroundAction(BrowserController controller, string encoding)
                : base(controller)
            {
                _encoding = encoding;
            }

            public override void run()
            {
                BrowserController.UnmountImpl();
            }

            public override void cleanup()
            {
                BrowserController._session.getHost().setEncoding(_encoding);
                BrowserController.View_RefreshBrowser();
            }

            public override string getActivity()
            {
                return String.Format(Locale.localizedString("Disconnecting {0}", "Status"),
                                     BrowserController._session.getHost().getHostname());
            }
        }

        internal class InterruptAction : BrowserBackgroundAction
        {
            private readonly Object _lock = new Object();
            private readonly Session _session;

            public InterruptAction(BrowserController controller, Session session)
                : base(controller)
            {
                _session = session;
            }

            public override void run()
            {
                if (BrowserController.HasSession())
                {
                    // Aggressively close the connection to interrupt the current task
                    _session.interrupt();
                }
            }

            public override void cleanup()
            {
                ;
            }

            public override int retry()
            {
                return 0;
            }

            public override object @lock()
            {
                return _lock;
            }

            public override string getActivity()
            {
                return String.Format(Locale.localizedString("Disconnecting {0}", "Status"),
                                     _session.getHost().getHostname());
            }
        }

        private class LazyTransferPrompt : TransferPrompt
        {
            private readonly BrowserController _controller;
            private readonly Transfer _transfer;

            public LazyTransferPrompt(BrowserController controller, Transfer transfer)
            {
                _transfer = transfer;
                _controller = controller;
            }

            public TransferAction prompt()
            {
                return TransferPromptController.Create(_controller, _transfer).prompt();
            }
        }

        private class MountAction : BrowserBackgroundAction
        {
            private readonly Host _host;
            private readonly Session _session;// sessao logada andre
            private Path _mount;

            public MountAction(BrowserController controller,
                               Session session,
                               Host host)
                : base(controller)
            {
                _host = host;
                _session = session;
            }

            public override void run()
            {
                // Mount this session
                try
                {
                    _mount = _session.mount();
                }
                catch (Exception e) {
                    System.Windows.Forms.MessageBox.Show(e.Message);
                }
            }

            public override void cleanup()
            {
                // Set the working directory
                BrowserController.SetWorkdir(_mount);
                if (!_session.isConnected())
                {
                    // Connection attempt failed
                    BrowserController.UnmountImpl();
                }
            }

            public override string getActivity()
            {
                return String.Format(Locale.localizedString("Mounting {0}", "Status"),
                                     _host.getHostname());
            }
        }

        internal class ProgessListener : ProgressListener
        {
            private readonly BrowserController _controller;
            private string _laststatus;

            public ProgessListener(BrowserController controller)
            {
                _controller = controller;
            }

            public string Laststatus
            {
                get { return _laststatus; }
            }

            public void message(string msg)
            {
                _laststatus = msg;
                AsyncDelegate updateLabel = delegate { _controller.View.StatusLabel = msg; };
                _controller.Invoke(updateLabel);
            }
        }

        internal class ProgressTransferAdapter : TransferAdapter
        {
            private const long Delay = 0;
            private const long Period = 500; //in milliseconds
            private readonly BrowserController _controller;
            private readonly Speedometer _meter;
            private readonly Timer _timer;
            private readonly Transfer _transfer;

            public ProgressTransferAdapter(BrowserController controller, Transfer transfer)
            {
                _meter = new Speedometer();
                _controller = controller;
                _timer = new Timer(timerCallback, null, Timeout.Infinite, Period);
                _transfer = transfer;
            }

            private void timerCallback(object state)
            {
                _controller.Invoke(
                    delegate
                        {
                            _controller.View.StatusLabel = _meter.getProgress(_transfer.isRunning(), _transfer.getSize(),
                                                                              _transfer.getTransferred());
                        });
            }

            public override void willTransferPath(Path path)
            {
                _meter.reset(_transfer.getTransferred());
                _timer.Change(Delay, Period);
            }

            public override void didTransferPath(Path path)
            {
                _timer.Change(Timeout.Infinite, Period);
                _meter.reset(_transfer.getTransferred());
            }

            public override void bandwidthChanged(BandwidthThrottle bandwidth)
            {
                _meter.reset(_transfer.getTransferred());
            }

            public override void transferDidEnd()
            {
                _transfer.removeListener(this);
            }

            internal class ProgressTimerRunnable : Runnable
            {
                private readonly BrowserController _controller;
                private readonly Speedometer _meter;
                private readonly Transfer _transfer;

                public ProgressTimerRunnable(BrowserController controller, Speedometer meter, Transfer transfer)
                {
                    _controller = controller;
                    _meter = meter;
                    _transfer = transfer;
                }

                public void run()
                {
                    AsyncDelegate mainAction =
                        delegate
                            {
                                _controller.View.StatusLabel = _meter.getProgress(_transfer.isRunning(),
                                                                                  _transfer.getSize(),
                                                                                  _transfer.getTransferred());
                            };
                    _controller.Invoke(mainAction);
                }
            }
        }

        internal class ReloadTransferAdapter : TransferAdapter
        {
            private readonly IList<Path> _changed;
            private readonly BrowserController _controller;
            private readonly Transfer _transfer;

            public ReloadTransferAdapter(BrowserController controller, Transfer transfer, IList<Path> changed)
            {
                _controller = controller;
                _transfer = transfer;
                _changed = changed;
            }

            public override void transferDidEnd()
            {
                if (!_transfer.isCanceled())
                {
                    if (!_transfer.isCanceled())
                    {
                        _controller.invoke(new ReloadAction(_controller, _changed));
                    }
                }
                _transfer.removeListener(this);
            }

            private class ReloadAction : WindowMainAction
            {
                private readonly IList<Path> _changed;

                public ReloadAction(BrowserController c, IList<Path> changed)
                    : base(c)
                {
                    _changed = changed;
                }

                public override bool isValid()
                {
                    return base.isValid() && ((BrowserController) Controller).IsConnected();
                }

                public override void run()
                {
                    ((BrowserController) Controller).RefreshParentPaths(_changed, _changed);
                }
            }
        }

        private class RevertPathAction : BrowserBackgroundAction
        {
            private readonly Path _selected;

            public RevertPathAction(BrowserController controller, Path selected)
                : base(controller)
            {
                _selected = selected;
            }

            public override void run()
            {
                if (isCanceled())
                {
                    return;
                }
                _selected.revert();
            }

            public override void cleanup()
            {
                BrowserController.RefreshParentPaths(new Collection<Path> {_selected});
            }

            public override string getActivity()
            {
                return String.Format(Locale.localizedString("Reverting {0}", "Status"), _selected.getName());
            }
        }

        private class TransferBrowserBackgroundAction : BrowserBackgroundAction
        {
            private readonly TransferPrompt _prompt;
            private readonly Transfer _transfer;

            public TransferBrowserBackgroundAction(BrowserController controller,
                                                   TransferPrompt prompt,
                                                   Transfer transfer)
                : base(controller)
            {
                _prompt = prompt;
                _transfer = transfer;
            }

            public override void run()
            {
                Log.debug("run: " + getActivity());
                TransferOptions options = new TransferOptions();
                options.closeSession = false;
                _transfer.start(_prompt, options);
            }

            public override void cleanup()
            {
                Log.debug("cleanup: " + getActivity());
                BrowserController.UpdateStatusLabel();
                base.cleanup();
            }

            public override void cancel()
            {
                Log.debug("cancel: " + getActivity());
                _transfer.cancel();
                base.cancel();
            }

            public override string getActivity()
            {
                return _transfer.getName();
            }
        }

        private class UnarchiveAction : BrowserBackgroundAction
        {
            private readonly Archive _archive;
            private readonly List<Path> _expanded;
            private readonly Path _selected;

            public UnarchiveAction(BrowserController controller, Archive archive, Path selected, List<Path> expanded)
                : base(controller)
            {
                _archive = archive;
                _expanded = expanded;
                _selected = selected;
            }

            public override void run()
            {
                BrowserController._session.unarchive(_archive, _selected);
            }

            public override string getActivity()
            {
                return _archive.getDecompressCommand(_selected);
            }

            public override void cleanup()
            {
                _expanded.AddRange(Utils.ConvertFromJavaList<Path>(_archive.getExpanded(new ArrayList {_selected})));
                BrowserController.RefreshParentPaths(_expanded, _expanded);
            }
        }

        private class UnmountAction : BrowserBackgroundAction
        {
            private readonly CallbackDelegate _callback;
            private readonly BrowserController _controller;

            public UnmountAction(BrowserController controller, CallbackDelegate callback)
                : base(controller)
            {
                _controller = controller;
                _callback = callback;
            }

            public override void run()
            {
                _controller.UnmountImpl();
            }

            public override void cleanup()
            {
                // Clear the cache on the main thread to make sure the browser model is not in an invalid state
                _controller._session.cache().clear();
                _controller._session.getHost().getCredentials().setPassword(null);

                _callback();
            }

            public override string getActivity()
            {
                return String.Format(Locale.localizedString("Disconnecting {0}", "Status"),
                                     _controller._session.getHost().getHostname());
            }
        }

        private class UpdateInspectorAction : BrowserBackgroundAction
        {
            private readonly IList<Path> _selected;

            public UpdateInspectorAction(BrowserController controller, IList<Path> selected)
                : base(controller)
            {
                _selected = selected;
            }


            public override void run()
            {
                foreach (Path path in _selected)
                {
                    if (isCanceled())
                    {
                        break;
                    }
                    if (path.attributes().getPermission() == null)
                    {
                        path.readUnixPermission();
                    }
                }
            }

            public override void cleanup()
            {
                if (BrowserController._inspector != null)
                {
                    BrowserController._inspector.Files = _selected;
                }
            }
        }

        private class WorkdirAction : BrowserBackgroundAction
        {
            private readonly Path _directory;
            private readonly List<Path> _selected;

            public WorkdirAction(BrowserController controller, Path directory, List<Path> selected)
                : base(controller)
            {
                _directory = directory;
                _selected = selected;
            }

            public override string getActivity()
            {
                return String.Format(Locale.localizedString("Listing directory {0}", "Status"),
                                     _directory.getName());
            }

            public override void cleanup()
            {
                // Remove any custom file filter
                BrowserController.SetPathFilter(null);

                BrowserController.UpdateNavigationPaths();

                // Mark the browser data source as dirty
                BrowserController.ReloadData(_selected);

                // Change to the browser view
                BrowserController.ToggleView(BrowserView.File);

                BrowserController.View.FocusBrowser();
            }

            public override void run()
            {
                AttributedList children = _directory.children();
                if (BrowserController.getSession().cache().isCached(_directory.getReference()))
                {
                    //Reset the readable attribute
                    children.attributes().setReadable(true);
                }
                // Get the directory listing in the background
                if (children.attributes().isReadable() || !children.isEmpty())
                {
                    // Update the working directory if listing is successful
                    BrowserController._workdir = _directory;
                    // Update the current working directory
                    BrowserController.AddPathToHistory(BrowserController.Workdir);
                }
            }
        }
    }
}