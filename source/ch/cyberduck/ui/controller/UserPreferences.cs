﻿// 
// Copyright (c) 2010-2012 Yves Langisch. All rights reserved.
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
using System.Globalization;
using System.IO;
using System.Reflection;
using System.Text.RegularExpressions;
using System.Windows.Forms;
using Ch.Cyberduck.Core;
using Ch.Cyberduck.Core.Editor;
using Ch.Cyberduck.Properties;
using ch.cyberduck.core;
using java.util;
using org.apache.log4j;
using Path = System.IO.Path;

namespace Ch.Cyberduck.Ui.Controller
{
    public class UserPreferences : Preferences
    {
        private static readonly Logger Log = Logger.getLogger(typeof (UserPreferences).FullName);
        private SettingsDictionary _settings;

        private UserPreferences()
        {
            ;
        }

        /// <summary>
        /// Roaming application data path
        /// </summary>
        private static string RoamingApplicationDataPath
        {
            get
            {
                return Path.Combine(Environment.GetFolderPath(
                    Environment.SpecialFolder.ApplicationData),
                                    instance().getProperty("application.name"));
            }
        }

        /// <summary>
        /// Local application data path
        /// </summary>
        private static string LocalApplicationDataPath
        {
            get
            {
                return Path.Combine(Environment.GetFolderPath(
                    Environment.SpecialFolder.LocalApplicationData),
                                    instance().getProperty("application.name"));
            }
        }

        /// <summary>
        /// Try to get an OS version specific download path:
        /// - XP : Desktop
        /// - Vista or later : Downloads folder in the user home directory 
        /// </summary>
        private string DefaultDownloadPath
        {
            get
            {
                string homePath = HomeFolder;
                if (!string.IsNullOrEmpty(homePath))
                {
                    string downloads = Path.Combine(homePath, "Downloads");
                    if (Directory.Exists(downloads))
                    {
                        return downloads;
                    }
                }
                // fallback is Desktop
                return Environment.GetFolderPath(Environment.SpecialFolder.Desktop);
            }
        }

        /// <summary>
        /// Get platform specific home directory
        /// </summary>
        public static string HomeFolder
        {
            get
            {
                string homePath = (Environment.OSVersion.Platform == PlatformID.Unix ||
                                   Environment.OSVersion.Platform == PlatformID.MacOSX)
                                      ? Environment.GetEnvironmentVariable("HOME")
                                      : Environment.GetEnvironmentVariable("USERPROFILE");
                return homePath;
            }
        }

        private static string ApplicationRevision
        {
            get { return Assembly.GetExecutingAssembly().GetName().Version.Revision.ToString(); }
        }

        private static string ApplicationVersion
        {
            get { return Application.ProductVersion; }
        }

        public override void setProperty(string property, string value)
        {
            Log.info("setProperty: " + property + "," + value);
            _settings[property] = value;
        }

        public override string locale()
        {
            return getProperty("application.language");
        }

        public override void setProperty(string str, List l)
        {
            throw new InvalidOperationException();
        }

        public override void deleteProperty(string property)
        {
            Log.debug("deleteProperty: " + property);
            _settings.Remove(property);
        }

        public override string getProperty(string property)
        {
            if (_settings.ContainsKey(property))
            {
                return _settings[property];
            }
            return getDefault(property);
        }

        public override string getDisplayName(string locale)
        {
            //cy is a special case as it is not available in the framework
            if ("cy".Equals(locale))
            {
                return "Welsh";
            }
            CultureInfo cultureInfo = CultureInfo.GetCultureInfo(locale.Replace('_', '-'));
            return cultureInfo.TextInfo.ToTitleCase(cultureInfo.NativeName);
        }

        public override List applicationLocales()
        {
            Assembly asm = Assembly.GetExecutingAssembly();
            string[] names = asm.GetManifestResourceNames();
            // the dots apparently come from the relative path in the msbuild file
            Regex regex = new Regex("Ch.Cyberduck\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.([^\\..]*).lproj\\.Localizable\\.strings");
            List<string> distinctNames = new List<string>();
            foreach (var name in names)
            {
                Match match = regex.Match(name);
                if (match.Groups.Count > 1)
                {
                    distinctNames.Add(match.Groups[1].Value);
                }
            }
            if (!HasEastAsianFontSupport())
            {
                distinctNames.Remove("ja");
                distinctNames.Remove("ko");
                distinctNames.Remove("ka");
                distinctNames.Remove("zh_CN");
                distinctNames.Remove("zh_TW");
            }
            return Utils.ConvertToJavaList(distinctNames);
        }

        private bool HasEastAsianFontSupport()
        {
            if (Utils.IsVistaOrLater)
            {
                return true;
            }
            return
                Convert.ToBoolean(NativeMethods.IsValidLocale(CultureInfo.CreateSpecificCulture("zh").LCID,
                                                              NativeConstants.LCID_INSTALLED));
        }

        public object GetSpecialObject(string property)
        {
            return Settings.Default[property];
        }

        public override void save()
        {
            Log.debug("Saving preferences");
            // re-set field to force save
            Settings.Default.CdSettings = _settings;
            Settings.Default.Save();
        }

        public override List systemLocales()
        {
            List locales = new ArrayList();
            //add current UI culture
            locales.add(CultureInfo.CurrentUICulture.Name);
            //add current system culture
            locales.add(Application.CurrentCulture.Name);
            return locales;
        }

        protected override void load()
        {
            Log.debug("Loading preferences");
            // upgrade settings for a new version
            if (Settings.Default.UpgradeSettings)
            {
                Settings.Default.Upgrade();
                Settings.Default.UpgradeSettings = false;
            }
            _settings = Settings.Default.CdSettings ?? new SettingsDictionary();
        }

        protected override void setDefaults()
        {
            defaults.put("application.name", Application.ProductName);
            defaults.put("application.version", ApplicationVersion);
            defaults.put("application.revision", ApplicationRevision);
            defaults.put("application.language", GetDefaultLanguage());
            defaults.put("application.language.custom", false.ToString());

            defaults.put("update.feed.release", "http://version.cyberduck.ch/changelog.wys");
            defaults.put("update.feed.beta", "http://version.cyberduck.ch/beta/changelog.wys");
            defaults.put("update.feed.nightly", "http://version.cyberduck.ch/nightly/changelog.wys");

            defaults.put("update.feed", "release");

            // Importers
            defaults.put("bookmark.import.winscp.location", Path.Combine(Environment.GetFolderPath(
                Environment.SpecialFolder.Programs), "WinSCP", "winscp.ini"));
            defaults.put("bookmark.import.filezilla.location", Path.Combine(Environment.GetFolderPath(
                Environment.SpecialFolder.ApplicationData), "FileZilla", "sitemanager.xml"));
            defaults.put("bookmark.import.smartftp.location", Path.Combine(Environment.GetFolderPath(
                Environment.SpecialFolder.ApplicationData), "SmartFTP", "Client 2.0", "Favorites"));
            defaults.put("bookmark.import.flashfxp3.location", Path.Combine(Environment.GetFolderPath(
                Environment.SpecialFolder.ApplicationData), "FlashFXP", "3", "Sites.dat"));
            defaults.put("bookmark.import.flashfxp4.location", Path.Combine(Environment.GetFolderPath(
                Environment.SpecialFolder.ApplicationData), "FlashFXP", "4", "Sites.dat"));
            defaults.put("bookmark.import.flashfxp4.common.location", Path.Combine(Environment.GetFolderPath(
                Environment.SpecialFolder.CommonApplicationData), "FlashFXP", "4", "Sites.dat"));
            defaults.put("bookmark.import.wsftp.location", Path.Combine(Environment.GetFolderPath(
                Environment.SpecialFolder.ApplicationData), "Ipswitch", "WS_FTP", "Sites"));
            defaults.put("bookmark.import.fireftp.location", Path.Combine(Environment.GetFolderPath(
                Environment.SpecialFolder.ApplicationData), "Mozilla", "Firefox", "Profiles"));
            defaults.put("bookmark.import.s3browser.location", Path.Combine(Environment.GetFolderPath(
                Environment.SpecialFolder.ApplicationData), "S3Browser", "settings.ini"));
            defaults.put("bookmark.import.crossftp.location", Path.Combine(HomeFolder, ".crossftp", "sites.xml"));
            defaults.put("bookmark.import.cloudberry.s3.location", Path.Combine(Environment.GetFolderPath(
                Environment.SpecialFolder.LocalApplicationData), "CloudBerry S3 Explorer for Amazon S3", "settings.list"));
            defaults.put("bookmark.import.cloudberry.google.location", Path.Combine(Environment.GetFolderPath(
                Environment.SpecialFolder.LocalApplicationData), "CloudBerry Explorer for Google Storage",
                                                                                    "settings.list"));
            defaults.put("bookmark.import.cloudberry.azure.location", Path.Combine(Environment.GetFolderPath(
                Environment.SpecialFolder.LocalApplicationData), "CloudBerry Explorer for Azure Blob Storage",
                                                                                   "settings.list"));

            base.setDefaults();

            defaults.put("logging.config", "log4j-windows.xml");
            defaults.put("logging", "error");            

            //disable reminder for procotol handler registration
            defaults.put("defaulthandler.reminder", false.ToString());
            defaults.put("application.support.path", RoamingApplicationDataPath);
            defaults.put("application.receipt.path", RoamingApplicationDataPath);
            defaults.put("application.bookmarks.path", "bookmarks");

            defaults.put("update.check.last", "0");

            defaults.put("queue.download.folder", DefaultDownloadPath);
            defaults.put("queue.upload.permissions.useDefault", true.ToString());
            defaults.put("queue.upload.changePermissions", true.ToString());

            defaults.put("queue.dock.badge", true.ToString());

            defaults.put("ssh.knownhosts", Path.Combine(RoamingApplicationDataPath, "known_hosts"));
            defaults.put("browser.enterkey.rename", false.ToString());
            defaults.put("terminal.command.ssh", Path.Combine(HomeFolder, "putty.exe"));
            defaults.put("terminal.command.ssh.args", "-ssh {0} {1}@{2} -t -P {3} -m \"{4}\"");

            defaults.put("editor.bundleIdentifier", new EditorFactory.Notepad().getIdentifier());

            //default browser toolbar set
            defaults.put("browser.toolbar", true.ToString());
            defaults.put("browser.toolbar.openconnection", true.ToString());
            defaults.put("browser.toolbar.quickconnect", true.ToString());
            defaults.put("browser.toolbar.action", true.ToString());
            defaults.put("browser.toolbar.info", true.ToString());
            defaults.put("browser.toolbar.refresh", true.ToString());
            defaults.put("browser.toolbar.edit", true.ToString());
            defaults.put("browser.toolbar.openinbrowser", false.ToString());
            defaults.put("browser.toolbar.openinterminal", false.ToString());
            defaults.put("browser.toolbar.newfolder", false.ToString());
            defaults.put("browser.toolbar.delete", false.ToString());
            defaults.put("browser.toolbar.download", false.ToString());
            defaults.put("browser.toolbar.upload", true.ToString());
            defaults.put("browser.toolbar.transfers", true.ToString());

            //default transfer toolbar set
            defaults.put("transfer.toolbar.resume", true.ToString());
            defaults.put("transfer.toolbar.reload", true.ToString());
            defaults.put("transfer.toolbar.stop", true.ToString());
            defaults.put("transfer.toolbar.remove", true.ToString());
            defaults.put("transfer.toolbar.cleanup", false.ToString());
            defaults.put("transfer.toolbar.log", false.ToString());
            defaults.put("transfer.toolbar.open", true.ToString());
            defaults.put("transfer.toolbar.show", true.ToString());

            // Resolve symbolic links downloading target file instead
            defaults.put("path.symboliclink.resolve", true.ToString());
            // Resolve local links uploading target file instead. Currently not supporting shortcuts on Windows.
            defaults.put("local.symboliclink.resolve", true.ToString());
        }

        public string GetDefaultLanguage()
        {
            List sysLocales = systemLocales();
            List appLocales = applicationLocales();
            for (int i = 0; i < sysLocales.size(); i++)
            {
                string s = (string) sysLocales.get(i);
                string match = TryToMatchLocale(s.Replace('-', '_'), appLocales);
                if (null != match)
                {
                    Log.debug(String.Format("Default locale is '{0}' for system locale '{1}'", match, s));
                    return match;
                }
            }
            //default to english
            Log.debug("Fallback to locale 'en'");
            return "en";
        }

        private string TryToMatchLocale(string sysLocale, List appLocales)
        {
            for (int i = 0; i < appLocales.size(); i++)
            {
                string l = (string) appLocales.get(i);
                if (l.Equals(sysLocale))
                {
                    //direct match
                    return l;
                }
                //remove region
                int m = sysLocale.IndexOf('_');
                if (m > 0)
                {
                    string country = sysLocale.Substring(0, m);
                    if (l.Equals(country))
                    {
                        return l;
                    }
                }
            }
            return null;
        }

        public static void Register()
        {
            PreferencesFactory.addFactory(ch.cyberduck.core.Factory.NATIVE_PLATFORM, new Factory());
        }

        private class Factory : PreferencesFactory
        {
            protected override object create()
            {
                return new UserPreferences();
            }
        }
    }
}