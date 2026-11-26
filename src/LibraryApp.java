import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * JFrame GUI. Every button calls a method in Database (JDBC).
 * @author Ahammed Zihad Khan
 * GitHub: https://github.com/ahammedzihadkhan
 */
public class LibraryApp extends JFrame {
    interface Action { void run() throws Exception; }

    static final Color NAVY = new Color(0x1E293B), SLATE = new Color(0x64748B), BG = new Color(0xF1F5F9),
            LINE = new Color(0xE2E8F0), ZEBRA = new Color(0xF8FAFC), SELECT = new Color(0xDBEAFE),
            BLUE = new Color(0x2563EB), GREEN = new Color(0x16A34A), RED = new Color(0xDC2626),
            ORANGE = new Color(0xEA580C), PURPLE = new Color(0x7C3AED);

    private final JTable booksTable = new JTable(), membersTable = new JTable(), issuesTable = new JTable();
    private final JTextField bTitle = field(13), bAuthor = field(11), bQty = field(3), bSearch = field(20);
    private final JTextField mName = field(16), mPhone = field(14);
    private final JComboBox<String> iMember = new JComboBox<>(), iBook = new JComboBox<>();
    private final JLabel[] stats = new JLabel[4];
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);

    public LibraryApp() {
        super("Library Management System");
        content.setOpaque(false);
        content.add(booksPanel(), "Books");
        content.add(membersPanel(), "Members");
        content.add(issuesPanel(), "Issue / Return");

        JPanel main = new JPanel(new BorderLayout(0, 18));
        main.setBackground(BG);
        main.setBorder(pad(22, 22, 22, 22));
        main.add(statsRow(), BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);
        add(sidebar(), BorderLayout.WEST);
        add(main, BorderLayout.CENTER);

        for (JTable t : new JTable[]{booksTable, membersTable, issuesTable}) styleTable(t);
        setSize(1180, 700);
        setMinimumSize(new Dimension(1000, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        refresh();
    }

    // ---------- layout ----------
    private JPanel sidebar() {
        JPanel s = new JPanel();
        s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS));
        s.setBackground(NAVY);
        s.setPreferredSize(new Dimension(220, 0));
        s.setBorder(pad(26, 16, 20, 16));

        JLabel logo = new JLabel("<html><span style='font-size:18px'><b>LIBRARY</b></span><br>" +
                "<span style='color:#94A3B8'>Management System</span></html>");
        logo.setForeground(Color.WHITE);
        s.add(logo);
        s.add(Box.createVerticalStrut(34));

        List<JButton> nav = new ArrayList<>();
        for (String name : new String[]{"Books", "Members", "Issue / Return"}) {
            JButton b = flat("   " + name, NAVY);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            b.addActionListener(e -> {
                nav.forEach(x -> x.setBackground(NAVY));
                b.setBackground(BLUE);
                cards.show(content, name);
                refresh();
            });
            nav.add(b);
            s.add(b);
            s.add(Box.createVerticalStrut(6));
        }
        nav.get(0).setBackground(BLUE);

        s.add(Box.createVerticalGlue());
        JLabel foot = new JLabel("Java  |  JDBC  |  Swing");
        foot.setForeground(SLATE);
        s.add(foot);
        for (Component c : s.getComponents()) ((JComponent) c).setAlignmentX(LEFT_ALIGNMENT);
        return s;
    }

    private JPanel statsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        String[] captions = {"Book Titles", "Members", "Books Issued", "Overdue (> " + Database.LOAN_DAYS + " days)"};
        Color[] colors = {BLUE, PURPLE, ORANGE, RED};
        for (int i = 0; i < 4; i++) {
            stats[i] = new JLabel("0");
            stats[i].setFont(stats[i].getFont().deriveFont(Font.BOLD, 28f));
            stats[i].setForeground(colors[i]);
            JLabel cap = new JLabel(captions[i]);
            cap.setForeground(SLATE);
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 5, 0, 0, colors[i]), pad(12, 18, 12, 18)));
            card.add(stats[i], BorderLayout.CENTER);
            card.add(cap, BorderLayout.SOUTH);
            row.add(card);
        }
        return row;
    }

    private JPanel page(String title, JTable table, JPanel... formRows) {
        JLabel h = new JLabel(title);
        h.setFont(h.getFont().deriveFont(Font.BOLD, 20f));
        h.setForeground(NAVY);

        JPanel form = new JPanel(new GridLayout(formRows.length, 1, 0, 4));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), pad(8, 8, 8, 8)));
        for (JPanel r : formRows) form.add(r);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(h, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);

        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(Color.WHITE);
        sp.setBorder(BorderFactory.createLineBorder(LINE));

        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setOpaque(false);
        p.add(top, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // ---------- Books ----------
    private JPanel booksPanel() {
        JPanel form = row(label("Title"), bTitle, label("Author"), bAuthor, label("Qty"), bQty,
                button("Add", GREEN, () -> Database.addBook(text(bTitle), text(bAuthor), qty())),
                button("Update", BLUE, () -> Database.updateBook(selectedId(booksTable), text(bTitle), text(bAuthor), qty())),
                button("Delete", RED, () -> Database.deleteBook(selectedId(booksTable))),
                button("Clear", SLATE, () -> clear(booksTable, bTitle, bAuthor, bQty)));
        JPanel search = row(label("Search title / author"), bSearch);
        bSearch.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { refresh(); }
        });

        booksTable.getSelectionModel().addListSelectionListener(e -> {
            int r = booksTable.getSelectedRow();
            if (r < 0) return;
            bTitle.setText(String.valueOf(booksTable.getValueAt(r, 1)));
            bAuthor.setText(String.valueOf(booksTable.getValueAt(r, 2)));
            bQty.setText(String.valueOf(booksTable.getValueAt(r, 3)));
        });
        return page("Books", booksTable, form, search);
    }

    // ---------- Members ----------
    private JPanel membersPanel() {
        JPanel form = row(label("Name"), mName, label("Phone"), mPhone,
                button("Add", GREEN, () -> Database.addMember(text(mName), mPhone.getText().trim())),
                button("Update", BLUE, () -> Database.updateMember(selectedId(membersTable), text(mName), mPhone.getText().trim())),
                button("Delete", RED, () -> Database.deleteMember(selectedId(membersTable))),
                button("Clear", SLATE, () -> clear(membersTable, mName, mPhone)));

        membersTable.getSelectionModel().addListSelectionListener(e -> {
            int r = membersTable.getSelectedRow();
            if (r < 0) return;
            mName.setText(String.valueOf(membersTable.getValueAt(r, 1)));
            mPhone.setText(String.valueOf(membersTable.getValueAt(r, 2)));
        });
        return page("Members", membersTable, form);
    }

    // ---------- Issue / Return ----------
    private JPanel issuesPanel() {
        iMember.setPreferredSize(new Dimension(170, 32));
        iBook.setPreferredSize(new Dimension(290, 32));
        JPanel form = row(label("Member"), iMember, label("Book"), iBook,
                button("Issue Book", GREEN, () -> Database.issueBook(comboId(iBook), comboId(iMember))),
                button("Return Selected", ORANGE, () -> Database.returnBook(selectedId(issuesTable))));
        return page("Issue / Return  (loan period " + Database.LOAN_DAYS + " days)", issuesTable, form);
    }

    // ---------- styling helpers ----------
    private static Border pad(int t, int l, int b, int r) { return BorderFactory.createEmptyBorder(t, l, b, r); }

    private static JTextField field(int cols) {
        JTextField f = new JTextField(cols);
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xCBD5E1)), pad(5, 8, 5, 8)));
        return f;
    }

    private static JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setForeground(SLATE);
        return l;
    }

    private static JPanel row(Component... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        p.setOpaque(false);
        for (Component c : items) p.add(c);
        return p;
    }

    private static JButton flat(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(pad(7, 16, 7, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void styleTable(JTable t) {
        t.setDefaultEditor(Object.class, null); // read-only
        t.setRowHeight(30);
        t.setShowVerticalLines(false);
        t.setGridColor(LINE);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tb, v, sel, false, r, c);
                setBorder(pad(0, 10, 0, 10));
                setBackground(sel ? SELECT : r % 2 == 0 ? Color.WHITE : ZEBRA);
                setForeground(NAVY);
                String col = tb.getColumnName(c);
                if (col.equals("Status")) {
                    setForeground("Overdue".equals(v) ? RED : "Issued".equals(v) ? ORANGE : GREEN);
                    setFont(getFont().deriveFont(Font.BOLD));
                }
                if (col.equals("Available") && v instanceof Number n && n.intValue() == 0) {
                    setForeground(RED);
                    setFont(getFont().deriveFont(Font.BOLD));
                }
                return this;
            }
        });
        JTableHeader h = t.getTableHeader();
        h.setPreferredSize(new Dimension(0, 36));
        h.setReorderingAllowed(false);
        h.setDefaultRenderer((tb, v, sel, foc, r, c) -> {
            JLabel l = new JLabel(String.valueOf(v));
            l.setOpaque(true);
            l.setBackground(NAVY);
            l.setForeground(Color.WHITE);
            l.setFont(l.getFont().deriveFont(Font.BOLD));
            l.setBorder(pad(0, 10, 0, 10));
            return l;
        });
    }

    // ---------- behaviour ----------
    /** Button that runs a DB action, shows any error, then reloads all tables. */
    private JButton button(String label, Color color, Action action) {
        JButton b = flat(label, color);
        b.addActionListener(e -> {
            try {
                action.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            refresh();
        });
        return b;
    }

    private void refresh() {
        try {
            setModel(booksTable, Database.books(bSearch.getText().trim()));
            setModel(membersTable, Database.members());
            setModel(issuesTable, Database.issues());
            fillCombo(iMember, Database.members());
            fillCombo(iBook, Database.books(""));
            int[] s = Database.stats();
            for (int i = 0; i < 4; i++) stats[i].setText(String.valueOf(s[i]));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void setModel(JTable t, javax.swing.table.TableModel m) {
        t.setModel(m);
        t.getColumnModel().getColumn(0).setMaxWidth(60);
    }

    private static void fillCombo(JComboBox<String> combo, javax.swing.table.TableModel m) {
        combo.removeAllItems();
        for (int r = 0; r < m.getRowCount(); r++) combo.addItem(m.getValueAt(r, 0) + " - " + m.getValueAt(r, 1));
    }

    private static void clear(JTable t, JTextField... fields) {
        for (JTextField f : fields) f.setText("");
        t.clearSelection();
    }

    private static int comboId(JComboBox<String> combo) {
        if (combo.getSelectedItem() == null) throw new IllegalStateException("Nothing to select.");
        return Integer.parseInt(combo.getSelectedItem().toString().split(" - ")[0]);
    }

    private static int selectedId(JTable t) {
        int r = t.getSelectedRow();
        if (r < 0) throw new IllegalStateException("Please select a row in the table first.");
        return Integer.parseInt(String.valueOf(t.getValueAt(r, 0)));
    }

    private static String text(JTextField f) {
        String s = f.getText().trim();
        if (s.isEmpty()) throw new IllegalArgumentException("Please fill in all required fields.");
        return s;
    }

    private int qty() {
        try {
            int q = Integer.parseInt(bQty.getText().trim());
            if (q < 0) throw new NumberFormatException();
            return q;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Quantity must be a whole number (0 or more).");
        }
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        FontUIResource font = new FontUIResource("Segoe UI", Font.PLAIN, 14);
        UIManager.getLookAndFeelDefaults().keySet().stream()
                .filter(k -> k.toString().endsWith(".font")).toList()
                .forEach(k -> UIManager.put(k, font));
        Database.init();
        SwingUtilities.invokeLater(() -> new LibraryApp().setVisible(true));
    }
}
