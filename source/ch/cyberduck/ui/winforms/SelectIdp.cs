using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;

namespace Ch.Cyberduck.ui.winforms
{
    public partial class SelectIdp : Form
    {
        String idPServer = "";

        public String IdPServer
        {
            get { return idPServer; }
            set { idPServer = value; }
        }
        public SelectIdp(java.util.List idPList)
        {

            InitializeComponent();

            this.listView1.View = View.Details;
            this.listView1.AllowColumnReorder = true;
            this.listView1.FullRowSelect = true;

            for (int i = 0; i < idPList.size(); i++)
            {
                ListViewItem item = new ListViewItem(idPList.get(i).ToString(), 0);
                listView1.Items.Add(item);
            }

          
            listView1.Refresh();

            this.groupBox1.Controls.Add(listView1);
        }

        private void btnOk_Click(object sender, EventArgs e)
        {
            if (listView1.SelectedItems.Count != 0)
            {
                idPServer = listView1.SelectedItems[0].Text;
                this.DialogResult = DialogResult.OK;
                this.Close();
            }
            else {
                System.Windows.Forms.MessageBox.Show("Please, Choose a server!");
            }
        }

        private void btnCancel_Click(object sender, EventArgs e)
        {
            this.DialogResult = DialogResult.Cancel;
            this.Close();
        }
    }
}
