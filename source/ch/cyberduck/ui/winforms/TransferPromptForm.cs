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
using System.Drawing;
using System.Windows.Forms;
using BrightIdeasSoftware;
using Ch.Cyberduck.Ui.Controller;
using Ch.Cyberduck.Ui.Winforms.Controls;
using ch.cyberduck.core;
using ch.cyberduck.core.i18n;
using ch.cyberduck.core.transfer;

namespace Ch.Cyberduck.Ui.Winforms
{
    public partial class TransferPromptForm : BaseForm, ITransferPromptView
    {
        private static readonly int MaxHeight = 800;
        private static readonly int MaxWidth = 800;
        private static readonly int MinHeight = 250;
        private static readonly int MinWidth = 450;
        private bool _expanded = true;
        private ListViewItem _lastSelectedListViewItem;

        public TransferPromptForm()
        {
            InitializeComponent();

            DoubleBuffered = true;
            MaximumSize = new Size(MaxWidth, MaxHeight + detailsTableLayoutPanel.Height);
            MinimumSize = new Size(MinWidth, MinHeight + detailsTableLayoutPanel.Height);

            browser.UseExplorerTheme = true;
            browser.UseTranslucentSelection = true;
            browser.OwnerDraw = true;
            browser.UseOverlays = false;
            browser.HeaderStyle = ColumnHeaderStyle.None;
            browser.ShowGroups = false;
            browser.ShowImagesOnSubItems = true;
            browser.TreeColumnRenderer = new BrowserRenderer();
            browser.SelectedRowDecoration = new ExplorerRowBorderDecoration();
            browser.MultiSelect = false;
            browser.FullRowSelect = true;
            browser.ItemsChanged += (sender, args) => ItemsChanged();

            //due to the checkbox feature the highlight bar is not being redrawn properly -> redraw the entire control instead
            //todo report this bug to the ObjectListView forum
            browser.SelectedIndexChanged += delegate
                {
                    if (null != browser.SelectedItem)
                    {
                        browser.Invalidate(browser.SelectedItem.Bounds);
                        if (null != _lastSelectedListViewItem)
                        {
                            browser.Invalidate(_lastSelectedListViewItem.Bounds);
                        }
                        _lastSelectedListViewItem = browser.SelectedItem;
                    }
                };

            ScaledImageRenderer sir = new ScaledImageRenderer();

            treeColumnWarning.Renderer = sir;
            treeColumnCreate.Renderer = sir;
            treeColumnSync.Renderer = sir;

            treeColumnName.FillsFreeSpace = true;

            toggleDetailsLabel.Text = String.Format("        {0}", Locale.localizedString("Details"));
            toggleDetailsLabel.Click += delegate { ToggleDetailsEvent(); };
            toggleDetailsLabel.MouseDown += delegate { toggleDetailsLabel.ImageIndex = (_expanded ? 2 : 5); };
            toggleDetailsLabel.MouseEnter += delegate { toggleDetailsLabel.ImageIndex = (_expanded ? 1 : 4); };
            toggleDetailsLabel.MouseLeave += delegate { toggleDetailsLabel.ImageIndex = (_expanded ? 0 : 3); };
            toggleDetailsLabel.MouseUp += delegate { toggleDetailsLabel.ImageIndex = (_expanded ? 1 : 4); };

            browser.Focus();
        }

        public override string[] BundleNames
        {
            get { return new[] {"Prompt"}; }
        }

        public void SetModel(IEnumerable<Path> model)
        {
            browser.ClearCachedInfo();
            browser.SetObjects(model);
            //browser.Roots = model;            
        }

        public void RefreshBrowserObject(Path path)
        {
            if (path != null)
            {
                browser.RefreshObject(path);
            }
            else
            {
                //browser.ReloadTree();
            }
        }

        public bool DetailsVisible
        {
            get { return _expanded; }
            set
            {
                if (_expanded != value)
                {
                    _expanded = value;
                    //todo make it flickerfree
                    //try http://stackoverflow.com/questions/487661/how-do-i-suspend-painting-for-a-control-and-its-children
                    SuspendLayout();
                    if (_expanded)
                    {
                        MaximumSize = new Size(MaxWidth, MaxHeight + detailsTableLayoutPanel.Height);
                        Height += detailsTableLayoutPanel.Height;
                        MinimumSize = new Size(MinWidth, MinHeight + detailsTableLayoutPanel.Height);
                        ResumeLayout();
                    }
                    else
                    {
                        MinimumSize = new Size(MinWidth, MinHeight);
                        Height -= detailsTableLayoutPanel.Height;
                        MaximumSize = new Size(MaxWidth, MaxHeight);
                    }

                    detailsTableLayoutPanel.Visible = _expanded;
                    ResumeLayout();
                    toggleDetailsLabel.ImageIndex = (_expanded ? 1 : 4);
                }
            }
        }

        public int NumberOfFiles
        {
            get { return browser.GetItemCount(); }
        }

        public void PopulateActions(IDictionary<TransferAction, string> actions)
        {
            comboBoxAction.Items.Clear();
            comboBoxAction.DataSource = new BindingSource(actions, null);
            comboBoxAction.DisplayMember = "Value";
            comboBoxAction.ValueMember = "Key";
        }

        public string Title
        {
            set { Text = value; }
        }

        public TransferAction SelectedAction
        {
            get { return (TransferAction) comboBoxAction.SelectedValue; }
            set { comboBoxAction.SelectedValue = value; }
        }

        public Path SelectedPath
        {
            get { return (Path) browser.SelectedObject; }
            set { browser.SelectedObject = value; }
        }

        public string LocalFileUrl
        {
            set { localFileUrl.Text = value; }
        }

        public string LocalFileSize
        {
            set { localFileSize.Text = value; }
        }

        public string LocalFileModificationDate
        {
            set { localFileModificationDate.Text = value; }
        }

        public string RemoteFileUrl
        {
            set { remoteFileUrl.Text = value; }
        }

        public string RemoteFileSize
        {
            set { remoteFileSize.Text = value; }
        }

        public string RemoteFileModificationDate
        {
            set { remoteFileModificationDate.Text = value; }
        }

        public CheckStateGetterDelegate ModelCheckStateGetter
        {
            set { browser.CheckStateGetter = value; }
        }

        public CheckStatePutterDelegate ModelCheckStateSetter
        {
            set { browser.CheckStatePutter = value; }
        }

        public TreeListView.CanExpandGetterDelegate ModelCanExpandDelegate
        {
            set { browser.CanExpandGetter = value; }
        }

        public TreeListView.ChildrenGetterDelegate ModelChildrenGetterDelegate
        {
            set { browser.ChildrenGetter = value; }
        }

        public TypedColumn<Path>.TypedAspectGetterDelegate ModelFilenameGetter
        {
            set { new TypedColumn<Path>(treeColumnName) {AspectGetter = value}; }
        }

        public TypedColumn<Path>.TypedImageGetterDelegate ModelIconGetter
        {
            set
            {
                new TypedColumn<Path>(treeColumnName)
                    {ImageGetter = value};
            }
        }

        public TypedColumn<Path>.TypedAspectGetterDelegate ModelSizeGetter
        {
            set { new TypedColumn<Path>(treeColumnSize) {AspectGetter = value}; }
        }

        public AspectToStringConverterDelegate ModelSizeAsStringGetter
        {
            set { treeColumnSize.AspectToStringConverter = value; }
        }

        public TypedColumn<Path>.TypedImageGetterDelegate ModelWarningGetter
        {
            set { new TypedColumn<Path>(treeColumnWarning) {ImageGetter = value}; }
        }

        public TypedColumn<Path>.TypedImageGetterDelegate ModelCreateGetter
        {
            set { new TypedColumn<Path>(treeColumnCreate) {ImageGetter = value}; }
        }

        public TypedColumn<Path>.TypedImageGetterDelegate ModelSyncGetter
        {
            set { new TypedColumn<Path>(treeColumnSync) {ImageGetter = value}; }
        }

        public MulticolorTreeListView.ActiveGetterDelegate ModelActiveGetter
        {
            set { browser.ActiveGetter = value; }
        }

        public event VoidHandler ChangedActionEvent = delegate { };
        public event VoidHandler ChangedSelectionEvent = delegate { };
        public event VoidHandler ToggleDetailsEvent = delegate { };
        public event VoidHandler ItemsChanged;

        public void StartActivityAnimation()
        {
            animation.Visible = true;
        }

        public void StopActivityAnimation()
        {
            animation.Visible = false;
        }

        public string StatusLabel
        {
            set { statusLabel.Text = value; }
        }

        private void comboBoxAction_SelectionChangeCommitted(object sender, EventArgs e)
        {
            ChangedActionEvent();
        }

        private void browser_SelectionChanged(object sender, EventArgs e)
        {
            ChangedSelectionEvent();
        }

        private class ScaledImageRenderer : BaseRenderer
        {
            protected override int DrawImage(Graphics g, Rectangle r, object imageSelector)
            {
                if (imageSelector is Image)
                {
                    Image image = imageSelector as Image;
                    int top = r.Y;
                    if (image.Size.Height < r.Height)
                        top += ((r.Height - image.Size.Height)/2);

                    //make sure that 72dpi images are being scaled correctly
                    g.DrawImage(image, new Rectangle(r.X, top, image.Width, image.Height));
                    return image.Width;
                }
                return base.DrawImage(g, r, imageSelector);
            }
        }
    }
}