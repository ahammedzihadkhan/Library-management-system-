import java.io.File;

/**
 * Quick check of the JDBC logic on a throwaway database. Run: java -cp "out;lib/sqlite-jdbc.jar" SelfTest
 * @author Ahammed Zihad Khan
 * GitHub: https://github.com/ahammedzihadkhan
 */
public class SelfTest {
    public static void main(String[] args) throws Exception {
        new File("test.db").delete();
        Database.url = "jdbc:sqlite:test.db";
        Database.init();

        check(Database.members().getRowCount() == 12, "12 sample members");
        check("Zihad".equals(Database.members().getValueAt(0, 1)), "first member is Zihad");
        check(Database.books("").getRowCount() == 15, "15 sample books");
        int seeded = Database.issues().getRowCount();
        int[] s = Database.stats();
        check(s[2] == 10 && s[3] == 2, "10 books out, 2 overdue in seed");
        for (int r = 0; r < 15; r++)
            check(((Number) Database.books("").getValueAt(r, 4)).intValue() >= 0, "seed never over-issues book " + (r + 1));

        Database.addBook("Test Book", "Tester", 1);
        int book = ((Number) Database.books("Test Book").getValueAt(0, 0)).intValue();
        Database.issueBook(book, 1);
        check(fails(() -> Database.issueBook(book, 2)), "cannot issue when no copies left");
        check(fails(() -> Database.updateBook(book, "x", "y", 0)), "cannot set qty below issued count");
        check(fails(() -> Database.deleteBook(book)), "cannot delete a book that has issue records");

        int issue = ((Number) Database.issues().getValueAt(0, 0)).intValue(); // newest first
        Database.returnBook(issue);
        check(fails(() -> Database.returnBook(issue)), "cannot return twice");
        Database.issueBook(book, 2); // available again after return
        check(Database.issues().getRowCount() == seeded + 2, "2 new issue records");

        new File("test.db").delete();
        System.out.println("ALL TESTS PASSED");
    }

    interface Step { void run() throws Exception; }

    static boolean fails(Step s) {
        try { s.run(); return false; } catch (Exception e) { return true; }
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("FAILED: " + what);
        System.out.println("ok - " + what);
    }
}
