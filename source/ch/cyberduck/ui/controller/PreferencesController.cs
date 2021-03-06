﻿// 
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
using Ch.Cyberduck.Core;
using Ch.Cyberduck.Ui.Winforms;
using Ch.Cyberduck.Ui.Winforms.Controls;
using StructureMap;
using ch.cyberduck.core;
using ch.cyberduck.core.editor;
using ch.cyberduck.core.formatter;
using ch.cyberduck.core.io;
using ch.cyberduck.core.local;
using ch.cyberduck.core.transfer;
using java.util;
using java.util.regex;
using org.apache.log4j;
using org.jets3t.service.model;
using Locale = ch.cyberduck.core.i18n.Locale;

namespace Ch.Cyberduck.Ui.Controller
{
    internal class PreferencesController : WindowController<IPreferencesView>, CollectionListener
    {
        private static readonly string ForFiles = Locale.localizedString("for Files", "Preferences");
        private static readonly string ForFolders = Locale.localizedString("for Folders", "Preferences");
        private static readonly Logger Log = Logger.getLogger(typeof (PreferencesController).FullName);

        private static readonly KeyValueIconTriple<Host, string> NoneBookmark =
            new KeyValueIconTriple<Host, string>(null, Locale.localizedString("None"), null);

        private static readonly String NullString = "null";

        private static readonly string UseBrowserSession = Locale.localizedString("Use browser connection");
        private static readonly string UseQueueSession = Locale.localizedString("Open new connection");
        private static PreferencesController _instance;

        private bool _downloadRegexInvalid;
        private bool _uploadRegexInvalid;

        private PreferencesController(IPreferencesView view)
        {
            View = view;
            Init();

            View.SaveWorkspaceChangedEvent += View_SaveWorkspaceChangedEvent;
            View.NewBrowserOnStartupChangedEvent += View_NewBrowserOnStartupChangedEvent;
            View.DefaultBookmarkChangedEvent += View_DefaultBookmarkChangedEvent;
            View.UseKeychainChangedEvent += View_UseKeychainChangedEvent;
            View.ConfirmDisconnectChangedEvent += View_ConfirmDisconnectChangedEvent;
            View.DefaultProtocolChangedEvent += View_DefaultProtocolChangedEvent;
            View.DefaultEditorChangedEvent += View_DefaultEditorChangedEvent;
            View.RepopulateEditorsEvent += View_RepopulateEditorsEvent;
            View.AlwaysUseDefaultEditorChangedEvent += View_AlwaysUseDefaultEditorChangedEvent;
            View.ShowHiddenFilesChangedEvent += View_ShowHiddenFilesChangedEvent;
            View.DoubleClickEditorChangedEvent += View_DoubleClickEditorChangedEvent;
            View.ReturnKeyRenamesChangedEvent += View_ReturnKeyRenamesChangedEvent;
            View.InfoWindowShowsCurrentSelectionChangedEvent += View_InfoWindowShowsCurrentSelectionChangedEvent;
            View.AlternatingRowBackgroundChangedEvent += View_AlternatingRowBackgroundChangedEvent;
            View.HorizontalLinesChangedEvent += View_HorizontalLinesChangedEvent;
            View.VerticalLinesChangedEvent += View_VerticalLinesChangedEvent;
            View.DefaultEncodingChangedEvent += View_DefaultEncodingChangedEvent;
            View.TransferModeChangedEvent += View_TransferModeChangedEvent;
            View.TransfersToFrontChangedEvent += View_TransfersToFrontChangedEvent;
            View.TransfersToBackChangedEvent += View_TransfersToBackChangedEvent;
            View.RemoveFromTransfersChangedEvent += View_RemoveFromTransfersChangedEvent;
            View.OpenAfterDownloadChangedEvent += View_OpenAfterDownloadChangedEvent;
            View.DownloadFolderChangedEvent += View_DownloadFolderChangedEvent;
            View.DuplicateDownloadActionChangedEvent += View_DuplicateDownloadActionChangedEvent;
            View.DuplicateUploadActionChangedEvent += View_DuplicateUploadActionChangedEvent;
            View.DuplicateDownloadOverwriteChangedEvent += View_DuplicateDownloadOverwriteChangedEvent;
            View.DuplicateUploadOverwriteChangedEvent += View_DuplicateUploadOverwriteChangedEvent;
            View.UploadWithTemporaryFilenameChangedEvent += View_UploadWithTemporaryFilenameChangedEvent;

            View.ChmodDownloadChangedEvent += View_ChmodDownloadChangedEvent;
            View.ChmodDownloadUseDefaultChangedEvent += View_ChmodDownloadUseDefaultChangedEvent;
            View.ChmodDownloadTypeChangedEvent += View_ChmodDownloadTypeChangedEvent;
            View.DownloadOwnerReadChangedEvent += View_DownloadDefaultPermissionsChangedEvent;
            View.DownloadOwnerWriteChangedEvent += View_DownloadDefaultPermissionsChangedEvent;
            View.DownloadOwnerExecuteChangedEvent += View_DownloadDefaultPermissionsChangedEvent;
            View.DownloadGroupReadChangedEvent += View_DownloadDefaultPermissionsChangedEvent;
            View.DownloadGroupWriteChangedEvent += View_DownloadDefaultPermissionsChangedEvent;
            View.DownloadGroupExecuteChangedEvent += View_DownloadDefaultPermissionsChangedEvent;
            View.DownloadOtherReadChangedEvent += View_DownloadDefaultPermissionsChangedEvent;
            View.DownloadOtherWriteChangedEvent += View_DownloadDefaultPermissionsChangedEvent;
            View.DownloadOtherExecuteChangedEvent += View_DownloadDefaultPermissionsChangedEvent;
            View.ChmodUploadChangedEvent += View_ChmodUploadChangedEvent;
            View.ChmodUploadUseDefaultChangedEvent += View_ChmodUploadUseDefaultChangedEvent;
            View.ChmodUploadTypeChangedEvent += View_ChmodUploadTypeChangedEvent;
            View.UploadOwnerReadChangedEvent += View_UploadDefaultPermissionsChangedEvent;
            View.UploadOwnerWriteChangedEvent += View_UploadDefaultPermissionsChangedEvent;
            View.UploadOwnerExecuteChangedEvent += View_UploadDefaultPermissionsChangedEvent;
            View.UploadGroupReadChangedEvent += View_UploadDefaultPermissionsChangedEvent;
            View.UploadGroupWriteChangedEvent += View_UploadDefaultPermissionsChangedEvent;
            View.UploadGroupExecuteChangedEvent += View_UploadDefaultPermissionsChangedEvent;
            View.UploadOtherReadChangedEvent += View_UploadDefaultPermissionsChangedEvent;
            View.UploadOtherWriteChangedEvent += View_UploadDefaultPermissionsChangedEvent;
            View.UploadOtherExecuteChangedEvent += View_UploadDefaultPermissionsChangedEvent;

            View.PreserveModificationDownloadChangedEvent += View_PreserveModificationDownloadChangedEvent;
            View.PreserveModificationUploadChangedEvent += View_PreserveModificationUploadChangedEvent;

            View.DownloadSkipChangedEvent += View_DownloadSkipChangedEvent;
            View.DownloadSkipRegexChangedEvent += View_DownloadSkipRegexChangedEvent;
            View.DownloadSkipRegexDefaultEvent += View_DownloadSkipRegexDefaultEvent;
            View.UploadSkipChangedEvent += View_UploadSkipChangedEvent;
            View.UploadSkipRegexChangedEvent += View_UploadSkipRegexChangedEvent;
            View.UploadSkipRegexDefaultEvent += View_UploadSkipRegexDefaultEvent;

            View.SshTransferChangedEvent += View_SshTransferChangedEvent;

            View.DefaultDownloadThrottleChangedEvent += View_DefaultDownloadThrottleChangedEvent;
            View.DefaultUploadThrottleChangedEvent += View_DefaultUploadThrottleChangedEvent;

            View.ConnectionTimeoutChangedEvent += View_ConnectionTimeoutChangedEvent;
            View.RetryDelayChangedEvent += View_RetryDelayChangedEvent;
            View.RetriesChangedEvent += View_RetriesChangedEvent;

            View.UseSystemProxyChangedEvent += View_UseSystemProxyChangedEvent;
            View.ChangeSystemProxyEvent += View_ChangeSystemProxyEvent;

            #region S3

            View.DefaultBucketLocationChangedEvent += View_DefaultBucketLocationChangedEvent;
            View.DefaultStorageClassChangedEvent += View_DefaultStorageClassChangedEvent;
            View.DefaultEncryptionChangedEvent += View_DefaultEncryptionChangedEvent;

            #endregion

            #region Google Docs

            View.DocumentExportFormatChanged += View_DocumentExportFormatChanged;
            View.PresentationExportFormatChanged += View_PresentationExportFormatChanged;
            View.SpreadsheetExportFormatChanged += View_SpreadsheetExportFormatChanged;
            View.ConvertUploadsChanged += View_ConvertUploadsChanged;
            View.OcrUploadsChanged += View_OcrUploadsChanged;

            #endregion

            #region Language

            View.LocaleChanged += View_LocaleChanged;

            #endregion

            #region Update

            View.AutomaticUpdateChangedEvent += View_AutomaticUpdateChangedEvent;
            View.CheckForUpdateEvent += View_CheckForUpdateEvent;
            View.UpdateFeedChangedEvent += View_UpdateFeedChangedEvent;

            #endregion
        }

        public static PreferencesController Instance
        {
            get
            {
                if (null == _instance)
                {
                    _instance = new PreferencesController(ObjectFactory.GetInstance<IPreferencesView>());
                }
                return _instance;
            }
        }

        public override bool Singleton
        {
            get { return true; }
        }

        public void collectionLoaded()
        {
            ;
        }

        public void collectionItemAdded(object obj)
        {
            Invoke(delegate
                {
                    Host selected = View.DefaultBookmark;
                    PopulateBookmarks();
                    SelectDefaultBookmark(selected);
                });
        }

        public void collectionItemRemoved(object obj)
        {
            Invoke(delegate
                {
                    Host selected = View.DefaultBookmark;
                    PopulateBookmarks();
                    SelectDefaultBookmark(selected);
                });
        }

        public void collectionItemChanged(object obj)
        {
            Invoke(delegate
                {
                    Host selected = View.DefaultBookmark;
                    PopulateBookmarks();
                    SelectDefaultBookmark(selected);
                });
        }

        private void View_DefaultEncryptionChangedEvent()
        {
            Preferences.instance().setProperty("s3.encryption.algorithm",
                                               NullString.Equals(View.DefaultEncryption) ? null : View.DefaultEncryption);
        }

        private void View_AlwaysUseDefaultEditorChangedEvent()
        {
            Preferences.instance().setProperty("editor.alwaysUseDefault", View.AlwaysUseDefaultEditor);
        }

        private void View_RepopulateEditorsEvent()
        {
            PopulateAndSelectEditor();
        }

        private void View_DefaultEditorChangedEvent()
        {
            Application selected = View.DefaultEditor;
            Preferences.instance().setProperty("editor.bundleIdentifier", selected.getIdentifier());
        }

        private void View_ChangeSystemProxyEvent()
        {
            LaunchIEOptions(4);
        }

        /// <summary>
        /// 
        /// </summary>
        /// <param name="activeRegister">Register to select (connections settings=4)</param>
        private void LaunchIEOptions(int activeRegister)
        {
            Utils.StartProcess("rundll32.exe", "shell32.dll,Control_RunDLL inetcpl.cpl,," + activeRegister);
        }

        private void View_UpdateFeedChangedEvent()
        {
            Preferences.instance().setProperty("update.feed", View.UpdateFeed);
        }

        private void View_UploadWithTemporaryFilenameChangedEvent()
        {
            Preferences.instance().setProperty("queue.upload.file.temporary", View.UploadWithTemporaryFilename);
        }

        private void View_UseSystemProxyChangedEvent()
        {
            Preferences.instance().setProperty("connection.proxy.enable", View.UseSystemProxy);
        }

        private void View_CheckForUpdateEvent()
        {
            UpdateController.Instance.ForceCheckForUpdates(false);
        }

        private void View_AutomaticUpdateChangedEvent()
        {
            Preferences.instance().setProperty("update.check", View.AutomaticUpdateCheck);
        }

        private void View_LocaleChanged()
        {
            if ("default".Equals(View.CurrentLocale))
            {
                Preferences.instance().deleteProperty("application.language");
                Preferences.instance().setProperty("application.language.custom", false.ToString());
            }
            else
            {
                Preferences.instance().setProperty("application.language", View.CurrentLocale);
                Preferences.instance().setProperty("application.language.custom", true.ToString());
            }
        }

        private void View_OcrUploadsChanged()
        {
            Preferences.instance().setProperty("google.docs.upload.ocr", View.OcrUploads);
        }

        private void View_ConvertUploadsChanged()
        {
            Preferences.instance().setProperty("google.docs.upload.convert", View.ConvertUploads);
        }

        private void View_SpreadsheetExportFormatChanged()
        {
            Preferences.instance().setProperty("google.docs.export.spreadsheet", View.SpreadsheetExportFormat);
        }

        private void View_PresentationExportFormatChanged()
        {
            Preferences.instance().setProperty("google.docs.export.presentation", View.PresentationExportFormat);
        }

        private void View_DocumentExportFormatChanged()
        {
            Preferences.instance().setProperty("google.docs.export.document", View.DocumentExportFormat);
        }

        private void View_DefaultStorageClassChangedEvent()
        {
            Preferences.instance().setProperty("s3.storage.class", View.DefaultStorageClass);
        }

        private void View_RetriesChangedEvent()
        {
            Preferences.instance().setProperty("connection.retry", View.Retries);
        }

        private void View_RetryDelayChangedEvent()
        {
            Preferences.instance().setProperty("connection.retry.delay", View.RetryDelay);
        }

        private void View_ConnectionTimeoutChangedEvent()
        {
            Preferences.instance().setProperty("connection.timeout.seconds", View.ConnectionTimeout);
        }

        private void View_DefaultUploadThrottleChangedEvent()
        {
            Preferences.instance().setProperty("queue.upload.bandwidth.bytes",
                                               View.DefaultUploadThrottle);
        }

        private void View_DefaultDownloadThrottleChangedEvent()
        {
            Preferences.instance().setProperty("queue.download.bandwidth.bytes",
                                               View.DefaultDownloadThrottle);
        }

        private void View_DefaultBucketLocationChangedEvent()
        {
            Preferences.instance().setProperty("s3.location", View.DefaultBucketLocation);
        }

        private void View_SshTransferChangedEvent()
        {
            if (View.SshTransfer.Equals(Protocol.SFTP.getDescription()))
            {
                Preferences.instance().setProperty("ssh.transfer", Protocol.SFTP.getIdentifier());
            }
            if (View.SshTransfer.Equals(Protocol.SCP.getDescription()))
            {
                Preferences.instance().setProperty("ssh.transfer", Protocol.SCP.getIdentifier());
            }
        }

        private void View_UploadSkipRegexDefaultEvent()
        {
            string regex = Preferences.instance().getProperty("queue.upload.skip.regex.default");
            View.UploadSkipRegex = regex;
            Preferences.instance().setProperty("queue.upload.skip.regex", regex);
        }

        private void View_UploadSkipRegexChangedEvent()
        {
            string value = View.UploadSkipRegex.Trim();
            if (string.IsNullOrEmpty(value))
            {
                Preferences.instance().setProperty("queue.upload.skip.enable", false);
                Preferences.instance().setProperty("queue.upload.skip.regex", value);
                View.UploadSkip = false;
            }
            try
            {
                Pattern compiled = Pattern.compile(value);
                Preferences.instance().setProperty("queue.upload.skip.regex", compiled.pattern());
                if (_uploadRegexInvalid)
                {
                    View.MarkUploadSkipRegex(-1);
                }
                _uploadRegexInvalid = false;
            }
            catch (PatternSyntaxException ex)
            {
                _uploadRegexInvalid = true;
                View.MarkUploadSkipRegex(ex.getIndex());
            }
        }

        private void View_UploadSkipChangedEvent()
        {
            Preferences.instance().setProperty("queue.upload.skip.enable", View.DownloadSkip);
            View.UploadSkipRegexEnabled = View.UploadSkip;
        }

        private void View_DownloadSkipRegexDefaultEvent()
        {
            string regex = Preferences.instance().getProperty("queue.download.skip.regex.default");
            View.DownloadSkipRegex = regex;
            Preferences.instance().setProperty("queue.download.skip.regex", regex);
        }

        private void View_DownloadSkipChangedEvent()
        {
            Preferences.instance().setProperty("queue.download.skip.enable", View.DownloadSkip);
            View.DownloadSkipRegexEnabled = View.DownloadSkip;
        }

        private void View_DownloadSkipRegexChangedEvent()
        {
            string value = View.DownloadSkipRegex.Trim();
            if (string.IsNullOrEmpty(value))
            {
                Preferences.instance().setProperty("queue.download.skip.enable", false);
                Preferences.instance().setProperty("queue.download.skip.regex", value);
                View.DownloadSkip = false;
            }
            try
            {
                Pattern compiled = Pattern.compile(value);
                Preferences.instance().setProperty("queue.download.skip.regex", compiled.pattern());
                if (_downloadRegexInvalid)
                {
                    View.MarkDownloadSkipRegex(-1);
                }
                _downloadRegexInvalid = false;
            }
            catch (PatternSyntaxException ex)
            {
                _downloadRegexInvalid = true;
                View.MarkDownloadSkipRegex(ex.getIndex());
            }
        }

        private void View_PreserveModificationUploadChangedEvent()
        {
            Preferences.instance().setProperty("queue.upload.preserveDate",
                                               View.PreserveModificationUpload);
        }

        private void View_PreserveModificationDownloadChangedEvent()
        {
            Preferences.instance().setProperty("queue.download.preserveDate",
                                               View.PreserveModificationDownload);
        }

        private void View_ChmodUploadTypeChangedEvent()
        {
            Permission p = null;
            if (ForFiles.Equals(View.ChmodUploadType))
            {
                p =
                    new Permission(
                        Preferences.instance().getInteger("queue.upload.permissions.file.default"));
            }
            if (ForFolders.Equals(View.ChmodUploadType))
            {
                p =
                    new Permission(
                        Preferences.instance().getInteger("queue.upload.permissions.folder.default"));
            }
            if (null == p)
            {
                Log.error("No selected item");
                return;
            }
            bool[] ownerPerm = p.getOwnerPermissions();
            bool[] groupPerm = p.getGroupPermissions();
            bool[] otherPerm = p.getOtherPermissions();

            View.UploadOwnerRead = ownerPerm[Permission.READ];
            View.UploadOwnerWrite = ownerPerm[Permission.WRITE];
            View.UploadOwnerExecute = ownerPerm[Permission.EXECUTE];

            View.UploadGroupRead = groupPerm[Permission.READ];
            View.UploadGroupWrite = groupPerm[Permission.WRITE];
            View.UploadGroupExecute = groupPerm[Permission.EXECUTE];

            View.UploadOtherRead = otherPerm[Permission.READ];
            View.UploadOtherWrite = otherPerm[Permission.WRITE];
            View.UploadOtherExecute = otherPerm[Permission.EXECUTE];
        }

        private void View_ChmodUploadUseDefaultChangedEvent()
        {
            Preferences.instance().setProperty("queue.upload.permissions.useDefault",
                                               View.ChmodUploadUseDefault);
            View.ChmodUploadDefaultEnabled = View.ChmodUploadUseDefault;
        }

        private void View_ChmodUploadChangedEvent()
        {
            Preferences.instance().setProperty("queue.upload.changePermissions", View.ChmodUpload);
            View.ChmodUploadEnabled = View.ChmodUpload;
        }

        private void View_ChmodDownloadTypeChangedEvent()
        {
            Permission p = null;
            if (ForFiles.Equals(View.ChmodDownloadType))
            {
                p =
                    new Permission(
                        Preferences.instance().getInteger("queue.download.permissions.file.default"));
            }
            if (ForFolders.Equals(View.ChmodDownloadType))
            {
                p =
                    new Permission(
                        Preferences.instance().getInteger("queue.download.permissions.folder.default"));
            }
            if (null == p)
            {
                Log.error("No selected item");
                return;
            }
            bool[] ownerPerm = p.getOwnerPermissions();
            bool[] groupPerm = p.getGroupPermissions();
            bool[] otherPerm = p.getOtherPermissions();

            View.DownloadOwnerRead = ownerPerm[Permission.READ];
            View.DownloadOwnerWrite = ownerPerm[Permission.WRITE];
            View.DownloadOwnerExecute = ownerPerm[Permission.EXECUTE];

            View.DownloadGroupRead = groupPerm[Permission.READ];
            View.DownloadGroupWrite = groupPerm[Permission.WRITE];
            View.DownloadGroupExecute = groupPerm[Permission.EXECUTE];

            View.DownloadOtherRead = otherPerm[Permission.READ];
            View.DownloadOtherWrite = otherPerm[Permission.WRITE];
            View.DownloadOtherExecute = otherPerm[Permission.EXECUTE];
        }

        private void View_ChmodDownloadUseDefaultChangedEvent()
        {
            Preferences.instance().setProperty("queue.download.permissions.useDefault",
                                               View.ChmodDownloadUseDefault);
            View.ChmodDownloadDefaultEnabled = View.ChmodDownloadUseDefault;
        }

        private void View_ChmodDownloadChangedEvent()
        {
            Preferences.instance().setProperty("queue.download.changePermissions", View.ChmodDownload);
            View.ChmodDownloadEnabled = View.ChmodDownload;
        }

        private void View_UploadDefaultPermissionsChangedEvent()
        {
            bool[][] p = new[] {new bool[3], new bool[3], new bool[3]};

            p[Permission.OWNER][Permission.READ] = View.UploadOwnerRead;
            p[Permission.OWNER][Permission.WRITE] = View.UploadOwnerWrite;
            p[Permission.OWNER][Permission.EXECUTE] = View.UploadOwnerExecute;

            p[Permission.GROUP][Permission.READ] = View.UploadGroupRead;
            p[Permission.GROUP][Permission.WRITE] = View.UploadGroupWrite;
            p[Permission.GROUP][Permission.EXECUTE] = View.UploadGroupExecute;

            p[Permission.OTHER][Permission.READ] = View.UploadOtherRead;
            p[Permission.OTHER][Permission.WRITE] = View.UploadOtherWrite;
            p[Permission.OTHER][Permission.EXECUTE] = View.UploadOtherExecute;

            Permission permission = new Permission(p);
            if (ForFiles.Equals(View.ChmodUploadType))
            {
                Preferences.instance().setProperty("queue.upload.permissions.file.default",
                                                   permission.getOctalString());
            }
            if (ForFolders.Equals(View.ChmodUploadType))
            {
                Preferences.instance().setProperty("queue.upload.permissions.folder.default",
                                                   permission.getOctalString());
            }
        }

        private void View_DownloadDefaultPermissionsChangedEvent()
        {
            bool[][] p = new[] {new bool[3], new bool[3], new bool[3]};

            p[Permission.OWNER][Permission.READ] = View.DownloadOwnerRead;
            p[Permission.OWNER][Permission.WRITE] = View.DownloadOwnerWrite;
            p[Permission.OWNER][Permission.EXECUTE] = View.DownloadOwnerExecute;

            p[Permission.GROUP][Permission.READ] = View.DownloadGroupRead;
            p[Permission.GROUP][Permission.WRITE] = View.DownloadGroupWrite;
            p[Permission.GROUP][Permission.EXECUTE] = View.DownloadGroupExecute;

            p[Permission.OTHER][Permission.READ] = View.DownloadOtherRead;
            p[Permission.OTHER][Permission.WRITE] = View.DownloadOtherWrite;
            p[Permission.OTHER][Permission.EXECUTE] = View.DownloadOtherExecute;

            Permission permission = new Permission(p);
            if (ForFiles.Equals(View.ChmodDownloadType))
            {
                Preferences.instance().setProperty("queue.download.permissions.file.default",
                                                   permission.getOctalString());
            }
            if (ForFolders.Equals(View.ChmodDownloadType))
            {
                Preferences.instance().setProperty("queue.download.permissions.folder.default",
                                                   permission.getOctalString());
            }
        }

        private void View_DuplicateUploadOverwriteChangedEvent()
        {
            if (View.DuplicateUploadOverwrite)
            {
                Preferences.instance().setProperty("queue.upload.reload.fileExists",
                                                   TransferAction.ACTION_OVERWRITE.toString());
            }
            else
            {
                Preferences.instance().setProperty("queue.upload.reload.fileExists",
                                                   Preferences.instance().
                                                               getProperty("queue.upload.fileExists"));
            }
        }

        private void View_DuplicateDownloadOverwriteChangedEvent()
        {
            if (View.DuplicateDownloadOverwrite)
            {
                Preferences.instance().setProperty("queue.download.reload.fileExists",
                                                   TransferAction.ACTION_OVERWRITE.toString());
            }
            else
            {
                Preferences.instance().setProperty("queue.download.reload.fileExists",
                                                   Preferences.instance().
                                                               getProperty("queue.download.fileExists"));
            }
        }

        private void View_DuplicateUploadActionChangedEvent()
        {
            duplicateComboboxClicked(View.DuplicateUploadAction, "queue.upload.fileExists");
            View_DuplicateUploadOverwriteChangedEvent();
        }

        private void duplicateComboboxClicked(String selected, String property)
        {
            if (selected.Equals(TransferAction.ACTION_CALLBACK.getLocalizableString()))
            {
                Preferences.instance().setProperty(property, TransferAction.ACTION_CALLBACK.toString());
            }
            else if (selected.Equals(TransferAction.ACTION_OVERWRITE.getLocalizableString()))
            {
                Preferences.instance().setProperty(property,
                                                   TransferAction.ACTION_OVERWRITE.toString());
            }
            else if (selected.Equals(TransferAction.ACTION_RESUME.getLocalizableString()))
            {
                Preferences.instance().setProperty(property, TransferAction.ACTION_RESUME.toString());
            }
            else if (selected.Equals(TransferAction.ACTION_RENAME.getLocalizableString()))
            {
                Preferences.instance().setProperty(property, TransferAction.ACTION_RENAME.toString());
            }
            else if (selected.Equals(TransferAction.ACTION_RENAME_EXISTING.getLocalizableString()))
            {
                Preferences.instance().setProperty(property, TransferAction.ACTION_RENAME_EXISTING.toString());
            }
            else if (selected.Equals(TransferAction.ACTION_SKIP.getLocalizableString()))
            {
                Preferences.instance().setProperty(property, TransferAction.ACTION_SKIP.toString());
            }
        }


        private void View_DuplicateDownloadActionChangedEvent()
        {
            duplicateComboboxClicked(View.DuplicateDownloadAction, "queue.download.fileExists");
            View_DuplicateDownloadOverwriteChangedEvent();
        }

        private void View_DownloadFolderChangedEvent()
        {
            Preferences.instance().setProperty("queue.download.folder", View.DownloadFolder);
        }

        private void View_OpenAfterDownloadChangedEvent()
        {
            Preferences.instance().setProperty("queue.postProcessItemWhenComplete",
                                               View.OpenAfterDownload);
        }

        private void View_RemoveFromTransfersChangedEvent()
        {
            Preferences.instance().setProperty("queue.removeItemWhenComplete",
                                               View.RemoveFromTransfers);
        }

        private void View_TransfersToBackChangedEvent()
        {
            Preferences.instance().setProperty("queue.orderBackOnStop", View.TransfersToBack);
        }

        private void View_TransfersToFrontChangedEvent()
        {
            Preferences.instance().setProperty("queue.orderFrontOnStart", View.TransfersToFront);
        }

        private void View_TransferModeChangedEvent()
        {
            if (UseBrowserSession.Equals(View.TransferMode))
            {
                Preferences.instance().setProperty("connection.host.max", 1);
            }
            else if (UseQueueSession.Equals(View.TransferMode))
            {
                Preferences.instance().setProperty("connection.host.max", -1);
            }
        }

        private void View_DefaultEncodingChangedEvent()
        {
            Preferences.instance().setProperty("browser.charset.encoding", View.DefaultEncoding);
        }

        private void View_VerticalLinesChangedEvent()
        {
            Preferences.instance().setProperty("browser.verticalLines", View.VerticalLines);
            //todo
            //BrowserController.updateBrowserTableAttributes();
        }

        private void View_HorizontalLinesChangedEvent()
        {
            Preferences.instance().setProperty("browser.horizontalLines", View.HorizontalLines);
            //todo
            //BrowserController.updateBrowserTableAttributes();
        }

        private void View_AlternatingRowBackgroundChangedEvent()
        {
            Preferences.instance().setProperty("browser.alternatingRows",
                                               View.AlternatingRowBackground);
            //todo
            //BrowserController.updateBrowserTableAttributes();
        }

        private void View_InfoWindowShowsCurrentSelectionChangedEvent()
        {
            Preferences.instance().setProperty("browser.info.isInspector",
                                               View.InfoWindowShowsCurrentSelection);
        }

        private void View_DoubleClickEditorChangedEvent()
        {
            Preferences.instance().setProperty("browser.doubleclick.edit", View.DoubleClickEditor);
        }

        private void View_ReturnKeyRenamesChangedEvent()
        {
            Preferences.instance().setProperty("browser.enterkey.rename", View.ReturnKeyRenames);
        }

        private void View_ShowHiddenFilesChangedEvent()
        {
            Preferences.instance().setProperty("browser.showHidden", View.ShowHiddenFiles);
        }

        private void View_DefaultProtocolChangedEvent()
        {
            Protocol selected = View.DefaultProtocol;
            Preferences.instance().setProperty("connection.protocol.default", selected.getProvider());
            Preferences.instance().setProperty("connection.port.default", selected.getDefaultPort());
        }

        private void View_ConfirmDisconnectChangedEvent()
        {
            Preferences.instance().setProperty("browser.confirmDisconnect",
                                               View.ConfirmDisconnect);
        }

        private void View_UseKeychainChangedEvent()
        {
            Preferences.instance().setProperty("connection.login.useKeychain", View.UseKeychain);
        }

        private void View_DefaultBookmarkChangedEvent()
        {
            if (null == View.DefaultBookmark)
            {
                Preferences.instance().deleteProperty("browser.defaultBookmark");
            }
            else
            {
                Preferences.instance().setProperty("browser.defaultBookmark",
                                                   View.DefaultBookmark.getNickname());
            }
        }

        private void SelectDefaultBookmark(Host host)
        {
            if (BookmarkCollection.defaultCollection().contains(host))
            {
                View.DefaultBookmark = host;
            }
            else
            {
                View.DefaultBookmark = null;
            }
        }

        private void SelectDefaultBookmark(string nickname)
        {
            if (null == nickname)
            {
                View.DefaultBookmark = null;
            }
            else
            {
                bool bookmarkFound = false;
                foreach (Host host in BookmarkCollection.defaultCollection())
                {
                    if (nickname.Equals(host.getNickname()))
                    {
                        View.DefaultBookmark = host;
                        bookmarkFound = true;
                        break;
                    }
                }
                if (!bookmarkFound)
                {
                    View.DefaultBookmark = null;
                }
            }
        }

        private void View_NewBrowserOnStartupChangedEvent()
        {
            Preferences.instance().setProperty("browser.openUntitled", View.NewBrowserOnStartup);
        }

        private void View_SaveWorkspaceChangedEvent()
        {
            Preferences.instance().setProperty("browser.serialize", View.SaveWorkspace);
        }

        private void Init()
        {
            #region General

            View.SaveWorkspace = Preferences.instance().getBoolean("browser.serialize");
            View.NewBrowserOnStartup = Preferences.instance().getBoolean("browser.openUntitled");
            PopulateBookmarks();
            BookmarkCollection.defaultCollection().addListener(this);
            View.ViewClosedEvent += delegate { BookmarkCollection.defaultCollection().removeListener(this); };
            SelectDefaultBookmark(Preferences.instance().getProperty("browser.defaultBookmark"));
            View.ConfirmDisconnect =
                Preferences.instance().getBoolean("browser.confirmDisconnect");
            View.UseKeychain = Preferences.instance().getBoolean("connection.login.useKeychain");
            PopulateDefaultProtocols();
            View.DefaultProtocol =
                ProtocolFactory.forName(Preferences.instance().getProperty("connection.protocol.default"));
            View.InfoWindowShowsCurrentSelection =
                Preferences.instance().getBoolean("browser.info.isInspector");
            View.ShowHiddenFiles = Preferences.instance().getBoolean("browser.showHidden");
            View.DoubleClickEditor = Preferences.instance().getBoolean("browser.doubleclick.edit");
            View.ReturnKeyRenames = Preferences.instance().getBoolean("browser.enterkey.rename");
            View.AlternatingRowBackground =
                Preferences.instance().getBoolean("browser.alternatingRows");
            View.VerticalLines = Preferences.instance().getBoolean("browser.verticalLines");
            View.HorizontalLines = Preferences.instance().getBoolean("browser.horizontalLines");
            PopulateEncodings();
            View.DefaultEncoding = Preferences.instance().getProperty("browser.charset.encoding");

            #endregion

            #region Transfers - General

            PopulateTransferModes();
            View.TransferMode = Preferences.instance().getInteger("connection.host.max") == 1
                                    ? UseBrowserSession
                                    : UseQueueSession;
            View.TransfersToFront = Preferences.instance().getBoolean("queue.orderFrontOnStart");
            View.TransfersToBack = Preferences.instance().getBoolean("queue.orderBackOnStop");
            View.RemoveFromTransfers =
                Preferences.instance().getBoolean("queue.removeItemWhenComplete");
            View.OpenAfterDownload =
                Preferences.instance().getBoolean("queue.postProcessItemWhenComplete");
            View.DownloadFolder = Preferences.instance().getProperty("queue.download.folder");
            PopulateDuplicateActions();
            View.DuplicateDownloadAction = GetDuplicateAction("queue.download.fileExists");
            View.DuplicateUploadAction = GetDuplicateAction("queue.upload.fileExists");
            View.DuplicateDownloadOverwrite = Preferences.instance().getProperty(
                "queue.download.reload.fileExists")
                                                         .
                                                          Equals(
                                                              TransferAction.ACTION_OVERWRITE.toString())
                                                  ? true
                                                  : false;
            View.DuplicateUploadOverwrite = Preferences.instance().getProperty(
                "queue.upload.reload.fileExists")
                                                       .
                                                        Equals(
                                                            TransferAction.ACTION_OVERWRITE.toString())
                                                ? true
                                                : false;
            View.UploadWithTemporaryFilename = Preferences.instance().getBoolean("queue.upload.file.temporary");

            #endregion

            #region Editor

            PopulateAndSelectEditor();
            View.AlwaysUseDefaultEditor = Preferences.instance().getBoolean("editor.alwaysUseDefault");

            #endregion

            #region Transfers - Permissions

            PopulateChmodTypes();
            View.ChmodDownload = Preferences.instance().getBoolean("queue.download.changePermissions");
            View.ChmodDownloadEnabled = View.ChmodDownload;
            View.ChmodDownloadUseDefault =
                Preferences.instance().getBoolean("queue.download.permissions.useDefault");
            View.ChmodDownloadDefaultEnabled = View.ChmodDownloadUseDefault;
            View.ChmodDownloadType = ForFiles;
            View_ChmodDownloadTypeChangedEvent();
            View.ChmodUpload = Preferences.instance().getBoolean("queue.upload.changePermissions");
            View.ChmodUploadEnabled = View.ChmodUpload;
            View.ChmodUploadUseDefault =
                Preferences.instance().getBoolean("queue.upload.permissions.useDefault");
            View.ChmodUploadDefaultEnabled = Preferences.instance().getBoolean("queue.upload.changePermissions") &&
                                             Preferences.instance().getBoolean("queue.upload.permissions.useDefault");
            View.ChmodUploadType = ForFiles;
            View_ChmodUploadTypeChangedEvent();

            #endregion

            #region Transfers - Timestamps

            View.PreserveModificationDownload =
                Preferences.instance().getBoolean("queue.download.preserveDate");
            View.PreserveModificationUpload =
                Preferences.instance().getBoolean("queue.upload.preserveDate");

            #endregion

            #region Transfers - Advanced

            View.DownloadSkip = Preferences.instance().getBoolean("queue.download.skip.enable");
            View.DownloadSkipRegex = Preferences.instance().getProperty("queue.download.skip.regex");
            View.DownloadSkipRegexEnabled = View.DownloadSkip;
            View.UploadSkip = Preferences.instance().getBoolean("queue.upload.skip.enable");
            View.UploadSkipRegex = Preferences.instance().getProperty("queue.upload.skip.regex");
            View.UploadSkipRegexEnabled = View.UploadSkip;

            PopulateSshTransfers();
            if (
                Preferences.instance().getProperty("ssh.transfer").Equals(
                    Protocol.SFTP.
                             getIdentifier()))
            {
                View.SshTransfer = Protocol.SFTP.getDescription();
            }
            else if (
                Preferences.instance().getProperty("ssh.transfer").Equals(
                    Protocol.SCP.
                             getIdentifier()))
            {
                View.SshTransfer = Protocol.SCP.getDescription();
            }

            PopulateDefaultDownloadThrottleList();
            PopulateDefaultUploadThrottleList();
            View.DefaultDownloadThrottle =
                Preferences.instance().getFloat("queue.download.bandwidth.bytes");
            View.DefaultUploadThrottle =
                Preferences.instance().getFloat("queue.upload.bandwidth.bytes");
            View.Retries = Preferences.instance().getInteger("connection.retry");
            View.RetryDelay = Preferences.instance().getInteger("connection.retry.delay");
            View.ConnectionTimeout = Preferences.instance().getInteger("connection.timeout.seconds");
            View.UseSystemProxy = Preferences.instance().getBoolean("connection.proxy.enable");

            #endregion

            #region S3

            PopulateDefaultBucketLocations();
            View.DefaultBucketLocation = Preferences.instance().getProperty("s3.location");
            PopulateDefaultStorageClasses();
            View.DefaultStorageClass = Preferences.instance().getProperty("s3.storage.class");
            PopulateDefaultEncryption();
            String algorithm = Preferences.instance().getProperty("s3.encryption.algorithm");
            View.DefaultEncryption = Utils.IsNotBlank(algorithm) ? algorithm : NullString;

            #endregion

            #region Google Docs

            PopulateDocumentExportFormats();
            View.DocumentExportFormat =
                Preferences.instance().getProperty("google.docs.export.document");
            PopulatePresentationExportFormats();
            View.PresentationExportFormat = Preferences.instance().getProperty("google.docs.export.presentation");
            PopulateSpreadsheetExportFormats();
            View.SpreadsheetExportFormat = Preferences.instance().getProperty("google.docs.export.spreadsheet");
            View.ConvertUploads = Preferences.instance().getBoolean("google.docs.upload.convert");
            View.OcrUploads = Preferences.instance().getBoolean("google.docs.upload.ocr");

            #endregion

            #region Update

            View.AutomaticUpdateCheck = Preferences.instance().getBoolean("update.check");
            long lastCheck = Preferences.instance().getLong("update.check.last");
            View.LastUpdateCheck = 0 == lastCheck
                                       ? String.Empty
                                       : UserDefaultsDateFormatter.GetLongFormat(
                                           new DateTime(Preferences.instance().getLong("update.check.last")));
            PopulateFeeds();
            View.UpdateFeed = Preferences.instance().getProperty("update.feed");

            #endregion

            #region Language

            PopulateLanguages();
            string userLanguage = Preferences.instance().getProperty("application.language");

            if (Preferences.instance().getBoolean("application.language.custom"))
            {
                View.CurrentLocale = userLanguage;
            }
            else
            {
                View.CurrentLocale = "default";
            }

            #endregion
        }

        private void PopulateDefaultEncryption()
        {
            IList<KeyValuePair<string, string>> algorithms = new List<KeyValuePair<string, string>>();
            algorithms.Add(new KeyValuePair<string, string>(NullString, Locale.localizedString("None")));
            algorithms.Add(new KeyValuePair<string, string>("AES256", Locale.localizedString("AES256", "S3")));
            View.PopulateDefaultEncryption(algorithms);
        }

        private void PopulateFeeds()
        {
            IList<KeyValuePair<string, string>> feeds = new List<KeyValuePair<string, string>>();
            feeds.Add(new KeyValuePair<string, string>(
                          "release",
                          Locale.localizedString("Release")));
            feeds.Add(new KeyValuePair<string, string>(
                          "beta",
                          Locale.localizedString("Beta")));
            feeds.Add(new KeyValuePair<string, string>(
                          "nightly",
                          Locale.localizedString("Snapshot Builds")));
            View.PopulateUpdateFeeds(feeds);
        }

        private void PopulateLanguages()
        {
            IList<KeyValuePair<string, string>> locales = new List<KeyValuePair<string, string>>();

            List appLocales = Preferences.instance().applicationLocales();
            locales.Add(new KeyValuePair<string, string>("default", Locale.localizedString("Default")));
            for (int i = 0; i < appLocales.size(); i++)
            {
                string locale = (string) appLocales.get(i);
                locales.Add(new KeyValuePair<string, string>(locale, Preferences.instance().getDisplayName(locale)));
            }
            View.PopulateLocales(locales);
        }


        private void PopulateSpreadsheetExportFormats()
        {
            IList<KeyValuePair<string, string>> f = new List<KeyValuePair<string, string>>();
            string formats = Preferences.instance().getProperty("google.docs.export.spreadsheet.formats");
            foreach (string s in formats.Split(','))
            {
                string ext = "." + s;
                f.Add(new KeyValuePair<string, string>(s,
                                                       String.Format("{0} ({1})",
                                                                     FileDescriptorFactory.get().getKind(ext), ext)));
            }
            View.PopulateSpreadsheetExportFormats(f);
        }

        private void PopulatePresentationExportFormats()
        {
            IList<KeyValuePair<string, string>> f = new List<KeyValuePair<string, string>>();
            string formats = Preferences.instance().getProperty("google.docs.export.presentation.formats");
            foreach (string s in formats.Split(','))
            {
                string ext = "." + s;
                f.Add(new KeyValuePair<string, string>(s,
                                                       String.Format("{0} ({1})",
                                                                     FileDescriptorFactory.get().getKind(ext), ext)));
            }
            View.PopulatePresentationExportFormats(f);
        }

        private void PopulateDocumentExportFormats()
        {
            IList<KeyValuePair<string, string>> f = new List<KeyValuePair<string, string>>();
            string formats = Preferences.instance().getProperty("google.docs.export.document.formats");
            foreach (string s in formats.Split(','))
            {
                string ext = "." + s;
                f.Add(new KeyValuePair<string, string>(s,
                                                       String.Format("{0} ({1})",
                                                                     FileDescriptorFactory.get().getKind(ext), ext)));
            }
            View.PopulateDocumentExportFormats(f);
        }

        private void PopulateDefaultUploadThrottleList()
        {
            IList<KeyValuePair<float, string>> list = new List<KeyValuePair<float, string>>();
            list.Add(new KeyValuePair<float, string>(BandwidthThrottle.UNLIMITED,
                                                     Locale.localizedString("Unlimited Bandwidth", "Preferences")));
            foreach (
                String option in
                    Preferences.instance().getProperty("queue.bandwidth.options").Split(new[] {','},
                                                                                        StringSplitOptions.
                                                                                            RemoveEmptyEntries))
            {
                list.Add(new KeyValuePair<float, string>(Convert.ToInt32(option.Trim()),
                                                         (SizeFormatterFactory.get().format(
                                                             Convert.ToInt32(option.Trim())) + "/s")));
            }
            View.PopulateDefaultUploadThrottleList(list);
        }

        private void PopulateDefaultDownloadThrottleList()
        {
            IList<KeyValuePair<float, string>> list = new List<KeyValuePair<float, string>>();
            list.Add(new KeyValuePair<float, string>(BandwidthThrottle.UNLIMITED,
                                                     Locale.localizedString("Unlimited Bandwidth", "Preferences")));
            foreach (
                String option in
                    Preferences.instance().getProperty("queue.bandwidth.options").Split(new[] {','},
                                                                                        StringSplitOptions.
                                                                                            RemoveEmptyEntries))
            {
                list.Add(new KeyValuePair<float, string>(Convert.ToInt32(option.Trim()),
                                                         (SizeFormatterFactory.get().format(
                                                             Convert.ToInt32(option.Trim())) + "/s")));
            }
            View.PopulateDefaultDownloadThrottleList(list);
        }

        private void PopulateDefaultBucketLocations()
        {
            IList<KeyValuePair<string, string>> defaultBucketLocations = new List<KeyValuePair<string, string>>();
            Set locations = Protocol.S3_SSL.getLocations();
            Iterator iter = locations.iterator();
            while (iter.hasNext())
            {
                string location = (string) iter.next();
                defaultBucketLocations.Add(new KeyValuePair<string, string>(location,
                                                                            Locale.localizedString(location, "S3")));
            }
            View.PopulateDefaultBucketLocations(defaultBucketLocations);
        }

        private void PopulateDefaultStorageClasses()
        {
            IList<KeyValuePair<string, string>> storageClasses = new List<KeyValuePair<string, string>>();
            storageClasses.Add(new KeyValuePair<string, string>(S3Object.STORAGE_CLASS_STANDARD,
                                                                Locale.localizedString(S3Object.STORAGE_CLASS_STANDARD,
                                                                                       "S3")));
            storageClasses.Add(new KeyValuePair<string, string>(S3Object.STORAGE_CLASS_REDUCED_REDUNDANCY,
                                                                Locale.localizedString(
                                                                    Locale.localizedString(
                                                                        S3Object.STORAGE_CLASS_REDUCED_REDUNDANCY, "S3"),
                                                                    "S3")));
            View.PopulateDefaultStorageClasses(storageClasses);
        }

        private void PopulateSshTransfers()
        {
            List<string> sshTransfers = new List<string>();
            sshTransfers.Add(Protocol.SFTP.getDescription());
            sshTransfers.Add(Protocol.SCP.getDescription());
            View.PopulateSshTransfers(sshTransfers);
        }

        private void PopulateChmodTypes()
        {
            List<string> chmodDownloadTypes = new List<string>();
            chmodDownloadTypes.Add(ForFiles);
            chmodDownloadTypes.Add(ForFolders);
            View.PopulateChmodDownloadTypes(chmodDownloadTypes);

            List<string> chmodUploadTypes = new List<string>();
            chmodUploadTypes.Add(ForFiles);
            chmodUploadTypes.Add(ForFolders);
            View.PopulateChmodUploadTypes(chmodUploadTypes);
        }

        private string GetDuplicateAction(string property)
        {
            string action = null;
            if (
                Preferences.instance().getProperty(property).Equals(
                    TransferAction.ACTION_CALLBACK.
                                   toString()))
            {
                action = TransferAction.ACTION_CALLBACK.getLocalizableString();
            }
            else if (
                Preferences.instance().getProperty(property).Equals(
                    TransferAction.
                        ACTION_OVERWRITE.
                        toString()))
            {
                action = TransferAction.ACTION_OVERWRITE.getLocalizableString();
            }
            else if (
                Preferences.instance().getProperty(property).Equals(
                    TransferAction.
                        ACTION_RESUME.
                        toString()))
            {
                action = TransferAction.ACTION_RESUME.getLocalizableString();
            }
            else if (
                Preferences.instance().getProperty(property).Equals(
                    TransferAction.
                        ACTION_RENAME.
                        toString()))
            {
                action = TransferAction.ACTION_RENAME.getLocalizableString();
            }
            else if (
                Preferences.instance().getProperty(property).Equals(
                    TransferAction.
                        ACTION_RENAME_EXISTING.
                        toString()))
            {
                action = TransferAction.ACTION_RENAME_EXISTING.getLocalizableString();
            }
            else if (
                Preferences.instance().getProperty(property).Equals(
                    TransferAction.
                        ACTION_SKIP
                                  .toString()))
            {
                action = TransferAction.ACTION_SKIP.getLocalizableString();
            }
            return action;
        }

        private void PopulateDuplicateActions()
        {
            List<string> downloadActions = new List<string>();
            downloadActions.Add(TransferAction.ACTION_CALLBACK.getLocalizableString());
            downloadActions.Add(TransferAction.ACTION_OVERWRITE.getLocalizableString());
            downloadActions.Add(TransferAction.ACTION_RESUME.getLocalizableString());
            downloadActions.Add(TransferAction.ACTION_RENAME.getLocalizableString());
            downloadActions.Add(TransferAction.ACTION_RENAME_EXISTING.getLocalizableString());
            downloadActions.Add(TransferAction.ACTION_SKIP.getLocalizableString());
            View.PopulateDuplicateDownloadActions(downloadActions);

            List<string> uploadActions = new List<string>();
            uploadActions.Add(TransferAction.ACTION_CALLBACK.getLocalizableString());
            uploadActions.Add(TransferAction.ACTION_OVERWRITE.getLocalizableString());
            uploadActions.Add(TransferAction.ACTION_RESUME.getLocalizableString());
            uploadActions.Add(TransferAction.ACTION_RENAME.getLocalizableString());
            uploadActions.Add(TransferAction.ACTION_RENAME_EXISTING.getLocalizableString());
            uploadActions.Add(TransferAction.ACTION_SKIP.getLocalizableString());
            View.PopulateDuplicateUploadActions(uploadActions);
        }

        private void PopulateTransferModes()
        {
            List<string> modes = new List<string>();
            modes.Add(UseQueueSession);
            modes.Add(UseBrowserSession);
            View.PopulateTransferModes(modes);
        }

        private void PopulateEncodings()
        {
            List<string> encodings = new List<string>();
            encodings.AddRange(Utils.AvailableCharsets());
            View.PopulateEncodings(encodings);
        }

        private void PopulateDefaultProtocols()
        {
            List<KeyValueIconTriple<Protocol, string>> protocols = new List<KeyValueIconTriple<Protocol, string>>();
            foreach (Protocol p in ProtocolFactory.getKnownProtocols().toArray(new Protocol[] {}))
            {
                protocols.Add(new KeyValueIconTriple<Protocol, string>(p, p.getDescription(), p.getProvider()));
            }
            View.PopulateProtocols(protocols);
        }

        private void PopulateBookmarks()
        {
            List<KeyValueIconTriple<Host, string>> bookmarks = new List<KeyValueIconTriple<Host, string>>();
            bookmarks.Add(NoneBookmark);
            foreach (Host host in BookmarkCollection.defaultCollection())
            {
                bookmarks.Add(new KeyValueIconTriple<Host, string>(host, host.getNickname(),
                                                                   host.getProtocol().getProvider()));
            }
            View.PopulateBookmarks(bookmarks);
        }

        private void PopulateAndSelectEditor()
        {
            List<KeyValueIconTriple<Application, string>> editors =
                new List<KeyValueIconTriple<Application, string>>();

            Application defaultEditor = EditorFactory.instance().getDefaultEditor();
            String defaultEditorLocation = null;
            if (defaultEditor != null && Utils.IsNotBlank(defaultEditor.getIdentifier()))
            {
                defaultEditorLocation = defaultEditor.getIdentifier();
            }
            bool defaultEditorAdded = false;

            foreach (Application editor in Utils.ConvertFromJavaList<Application>(EditorFactory.instance().getEditors())
                )
            {
                if (ApplicationFinderFactory.get().isInstalled(editor))
                {
                    editors.Add(new KeyValueIconTriple<Application, string>(editor, editor.getName(),
                                                                            editor.getName()));
                    if (defaultEditorLocation != null && editor.getIdentifier().Equals(defaultEditorLocation))
                    {
                        defaultEditorAdded = true;
                    }
                }
            }
            if (!defaultEditorAdded)
            {
                if (defaultEditor != null && ApplicationFinderFactory.get().isInstalled(defaultEditor))
                {
                    editors.Insert(0,
                                   new KeyValueIconTriple<Application, string>(defaultEditor,
                                                                               defaultEditor.getName(),
                                                                               defaultEditor.getName()));
                }
            }
            editors.Add(new KeyValueIconTriple<Application, string>(new Application(null, null),
                                                                    Locale.localizedString("Choose") + "…",
                                                                    String.Empty));
            View.PopulateEditors(editors);
            if (defaultEditor != null)
            {
                View.DefaultEditor = defaultEditor;
            }
            else
            {
                //dummy editor which leads to an empty selection
                View.DefaultEditor = new Application(null, null);
            }
        }
    }
}