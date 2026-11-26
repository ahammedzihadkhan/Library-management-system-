import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.time.LocalDate;

/**
 * All JDBC code lives here: connection, table creation, and every SQL query.
 * @author Ahammed Zihad Khan
 * GitHub: https://github.com/ahammedzihadkhan
 */
public class Database {
    static String url = "jdbc:sqlite:library.db";

    static Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON"); // SQLite enables FK checks per connection
        }
        return c;
    }

    /** Creates the tables on first run and inserts sample data. */
    static void init() throws SQLException {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS books (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, author TEXT NOT NULL, " +
                    "quantity INTEGER NOT NULL CHECK (quantity >= 0))");
            st.execute("CREATE TABLE IF NOT EXISTS members (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT)");
            st.execute("CREATE TABLE IF NOT EXISTS issues (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "book_id INTEGER NOT NULL REFERENCES books(id), " +
                    "member_id INTEGER NOT NULL REFERENCES members(id), " +
                    "issue_date TEXT NOT NULL, return_date TEXT)");

            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM members")) {
                if (rs.getInt(1) > 0) return; // already seeded
            }
            seed();
        }
    }

    /** Synthetic sample data (names, phones and dates are made up). */
    private static void seed() throws SQLException {
        String[] members = {"Zihad", "Navid", "Nishi", "Ayesha Rahman", "Tanvir Hasan", "Farhana Akter",
                "Rafi Ahmed", "Sadia Islam", "Imran Hossain", "Mitu Khatun", "Shakib Chowdhury", "Nusrat Jahan"};
        for (int i = 0; i < members.length; i++)
            addMember(members[i], String.format("0171%d-%06d", i % 10, 100231 + i * 7919));

        Object[][] books = {
                {"Java: The Complete Reference", "Herbert Schildt", 4},
                {"Head First Java", "Kathy Sierra", 3},
                {"Effective Java", "Joshua Bloch", 2},
                {"Database System Concepts", "Silberschatz", 1},
                {"Clean Code", "Robert C. Martin", 3},
                {"Introduction to Algorithms", "Thomas H. Cormen", 2},
                {"Operating System Concepts", "Abraham Silberschatz", 2},
                {"Computer Networking: A Top-Down Approach", "Kurose & Ross", 2},
                {"Design Patterns", "Erich Gamma", 1},
                {"The Pragmatic Programmer", "Andrew Hunt", 2},
                {"Data Structures Using C", "Reema Thareja", 5},
                {"Let Us C", "Yashavant Kanetkar", 4},
                {"Discrete Mathematics", "Kenneth H. Rosen", 3},
                {"Computer Organization and Design", "Patterson & Hennessy", 1},
                {"Artificial Intelligence: A Modern Approach", "Russell & Norvig", 2},
        };
        for (Object[] b : books) addBook((String) b[0], (String) b[1], (Integer) b[2]);

        // {book_id, member_id, days ago issued, days ago returned (-1 = still out)}
        int[][] issues = {
                {1, 1, 40, 30}, {2, 2, 35, 20}, {4, 3, 25, 10}, {5, 4, 22, -1}, {6, 5, 18, 5},
                {1, 6, 16, -1}, {8, 7, 12, -1}, {3, 1, 9, -1}, {11, 8, 7, 2}, {12, 9, 5, -1},
                {2, 10, 4, -1}, {15, 2, 3, -1}, {13, 11, 2, -1}, {10, 3, 1, -1}, {7, 12, 1, -1},
        };
        LocalDate today = LocalDate.now();
        for (int[] x : issues)
            exec("INSERT INTO issues (book_id, member_id, issue_date, return_date) VALUES (?, ?, ?, ?)",
                    x[0], x[1], today.minusDays(x[2]).toString(), x[3] < 0 ? null : today.minusDays(x[3]).toString());
    }

    /** Header counters: total titles, members, books currently out, overdue. */
    static int[] stats() throws SQLException {
        DefaultTableModel m = table("SELECT (SELECT COUNT(*) FROM books), (SELECT COUNT(*) FROM members), " +
                "(SELECT COUNT(*) FROM issues WHERE return_date IS NULL), " +
                "(SELECT COUNT(*) FROM issues WHERE return_date IS NULL AND julianday('now') - julianday(issue_date) > " + LOAN_DAYS + ")");
        int[] s = new int[4];
        for (int i = 0; i < 4; i++) s[i] = ((Number) m.getValueAt(0, i)).intValue();
        return s;
    }

    static final int LOAN_DAYS = 14;

    /** Runs INSERT / UPDATE / DELETE with a PreparedStatement; returns rows affected. */
    static int exec(String sql, Object... params) throws SQLException {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            return ps.executeUpdate();
        }
    }

    /** Runs a SELECT and copies the ResultSet into a table model for a JTable. */
    static DefaultTableModel table(String sql, Object... params) throws SQLException {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                DefaultTableModel model = new DefaultTableModel();
                for (int i = 1; i <= md.getColumnCount(); i++) model.addColumn(md.getColumnLabel(i));
                while (rs.next()) {
                    Object[] row = new Object[md.getColumnCount()];
                    for (int i = 0; i < row.length; i++) row[i] = rs.getObject(i + 1);
                    model.addRow(row);
                }
                return model;
            }
        }
    }

    // ---------- Books ----------
    static void addBook(String title, String author, int qty) throws SQLException {
        exec("INSERT INTO books (title, author, quantity) VALUES (?, ?, ?)", title, author, qty);
    }

    static void updateBook(int id, String title, String author, int qty) throws SQLException {
        int n = exec("UPDATE books SET title = ?, author = ?, quantity = ? WHERE id = ? " +
                "AND ? >= (SELECT COUNT(*) FROM issues WHERE book_id = ? AND return_date IS NULL)",
                title, author, qty, id, qty, id);
        if (n == 0) throw new SQLException("Quantity cannot be less than the copies currently issued.");
    }

    static void deleteBook(int id) throws SQLException {
        exec("DELETE FROM books WHERE id = ?", id);
    }

    static DefaultTableModel books(String search) throws SQLException {
        String like = "%" + search + "%";
        return table("SELECT b.id AS ID, b.title AS Title, b.author AS Author, b.quantity AS Total, " +
                "b.quantity - (SELECT COUNT(*) FROM issues i WHERE i.book_id = b.id AND i.return_date IS NULL) AS Available " +
                "FROM books b WHERE b.title LIKE ? OR b.author LIKE ? ORDER BY b.id", like, like);
    }

    // ---------- Members ----------
    static void addMember(String name, String phone) throws SQLException {
        exec("INSERT INTO members (name, phone) VALUES (?, ?)", name, phone);
    }

    static void updateMember(int id, String name, String phone) throws SQLException {
        exec("UPDATE members SET name = ?, phone = ? WHERE id = ?", name, phone, id);
    }

    static void deleteMember(int id) throws SQLException {
        exec("DELETE FROM members WHERE id = ?", id);
    }

    static DefaultTableModel members() throws SQLException {
        return table("SELECT m.id AS ID, m.name AS Name, m.phone AS Phone, " +
                "(SELECT COUNT(*) FROM issues i WHERE i.member_id = m.id AND i.return_date IS NULL) AS \"Books Out\" " +
                "FROM members m ORDER BY m.id");
    }

    // ---------- Issue / Return ----------
    /** Issues a book inside a transaction: check stock, then insert. */
    static void issueBook(int bookId, int memberId) throws SQLException {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try (PreparedStatement check = c.prepareStatement(
                    "SELECT quantity - (SELECT COUNT(*) FROM issues WHERE book_id = ? AND return_date IS NULL) FROM books WHERE id = ?")) {
                check.setInt(1, bookId);
                check.setInt(2, bookId);
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) <= 0) {
                        c.rollback();
                        throw new SQLException("No copies of this book are available.");
                    }
                }
            }
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO issues (book_id, member_id, issue_date) VALUES (?, ?, ?)")) {
                ins.setInt(1, bookId);
                ins.setInt(2, memberId);
                ins.setString(3, LocalDate.now().toString());
                ins.executeUpdate();
            }
            c.commit();
        }
    }

    static void returnBook(int issueId) throws SQLException {
        int n = exec("UPDATE issues SET return_date = ? WHERE id = ? AND return_date IS NULL",
                LocalDate.now().toString(), issueId);
        if (n == 0) throw new SQLException("This book is already returned.");
    }

    static DefaultTableModel issues() throws SQLException {
        return table("SELECT i.id AS ID, m.name AS Member, b.title AS Book, i.issue_date AS Issued, " +
                "COALESCE(i.return_date, '-') AS Returned, " +
                "CASE WHEN i.return_date IS NOT NULL THEN 'Returned' " +
                "WHEN julianday('now') - julianday(i.issue_date) > " + LOAN_DAYS + " THEN 'Overdue' ELSE 'Issued' END AS Status " +
                "FROM issues i JOIN members m ON m.id = i.member_id JOIN books b ON b.id = i.book_id ORDER BY i.id DESC");
    }
}
