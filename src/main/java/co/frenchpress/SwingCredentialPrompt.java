package co.frenchpress;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Built-in {@link CredentialPrompt} for desktop (Linux/macOS/Windows) JVM
 * environments where AWT/Swing is present and a display is available.
 *
 * <p>Not instantiated on Android or headless servers — {@link #createIfSupported}
 * returns {@code null} in those environments so the caller can fall through to
 * a different strategy (env vars, etc.).
 */
public final class SwingCredentialPrompt implements CredentialPrompt {

  /**
   * @return a new instance if AWT/Swing is present and a display is available,
   *         otherwise {@code null}
   */
  static SwingCredentialPrompt createIfSupported () {
    try {
      if (GraphicsEnvironment.isHeadless()) return null;
      Class.forName("javax.swing.JDialog"); // absent on Android
      return new SwingCredentialPrompt();
    } catch (Throwable t) {
      return null;
    }
  }

  // -------------------------------------------------------------------------
  // CredentialPrompt
  // -------------------------------------------------------------------------

  @Override public Credentials promptForLogin () {
    Credentials[] result = { null };
    runOnEdt(() -> result[0] = buildLoginDialog());
    return result[0];
  }

  @Override public String promptForDeviceCode (boolean prevWrong) {
    String intro = prevWrong ? "The previous code was not accepted.\n\n" : "";
    String msg = intro
      + "Approve the sign-in request from your Steam Mobile App,\n"
      + "or enter your authenticator code below and click OK.";
    return showCodeDialog(msg);
  }

  @Override public String promptForEmailCode (String email, boolean prevWrong) {
    String intro = prevWrong ? "The previous code was not accepted.\n\n" : "";
    String msg = intro
      + "Approve the sign-in request from your Steam Mobile App,\n"
      + "or enter the code sent to " + email + " below and click OK.";
    return showCodeDialog(msg);
  }

  // -------------------------------------------------------------------------
  // Login dialog
  // -------------------------------------------------------------------------

  private static Credentials buildLoginDialog () {
    JDialog dialog = new JDialog((Frame) null, "Steam Login — Spiral Knights", true);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

    JTextField userField = new JTextField(22);
    JPasswordField passField = new JPasswordField(22);

    Credentials[] result = { null }; // null = web account (dialog closed or button clicked)

    JButton signInBtn = new JButton("Sign In");
    JButton webBtn    = new JButton("Use Web Account");

    signInBtn.addActionListener(e -> {
      result[0] = new Credentials(
        userField.getText().trim(),
        new String(passField.getPassword()));
      dialog.dispose();
    });
    webBtn.addActionListener(e -> dialog.dispose()); // result stays null

    // Enter in username field → move focus to password
    userField.addActionListener(e -> passField.requestFocusInWindow());
    // Enter in password field → sign in
    passField.addActionListener(e -> signInBtn.doClick());

    // Hint label
    JLabel hint = new JLabel(
      "<html>Enter your Steam credentials, or click <b>Use Web Account</b>"
      + "<br>to log in with Three Rings / Grey Havens credentials.</html>");
    hint.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

    // Form grid
    JPanel form = new JPanel(new GridBagLayout());
    GridBagConstraints g = new GridBagConstraints();
    g.insets = new Insets(3, 4, 3, 4);
    g.anchor = GridBagConstraints.WEST;

    g.gridx = 0; g.gridy = 0; form.add(new JLabel("Steam Username:"), g);
    g.gridx = 1;               form.add(userField, g);
    g.gridx = 0; g.gridy = 1; form.add(new JLabel("Password:"), g);
    g.gridx = 1;               form.add(passField, g);

    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
    buttons.add(webBtn);
    buttons.add(signInBtn);

    JPanel content = new JPanel(new BorderLayout(0, 0));
    content.setBorder(BorderFactory.createEmptyBorder(14, 18, 10, 18));
    content.add(hint,    BorderLayout.NORTH);
    content.add(form,    BorderLayout.CENTER);
    content.add(buttons, BorderLayout.SOUTH);

    dialog.setContentPane(content);
    dialog.getRootPane().setDefaultButton(signInBtn);
    dialog.pack();
    dialog.setLocationRelativeTo(null);
    dialog.setVisible(true); // blocks until disposed

    return result[0];
  }

  // -------------------------------------------------------------------------
  // 2FA code dialog (device code + email code share this layout)
  // -------------------------------------------------------------------------

  private static String showCodeDialog (String message) {
    String[] result = { "" }; // empty = wait for app approval
    runOnEdt(() -> result[0] = buildCodeDialog(message));
    return result[0];
  }

  private static String buildCodeDialog (String message) {
    JDialog dialog = new JDialog((Frame) null,
      "Steam Verification — Spiral Knights", true);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

    JTextArea msgArea = new JTextArea(message);
    msgArea.setEditable(false);
    msgArea.setOpaque(false);
    msgArea.setFocusable(false);
    msgArea.setLineWrap(true);
    msgArea.setWrapStyleWord(true);
    msgArea.setFont(UIManager.getFont("Label.font"));

    JTextField codeField = new JTextField(14);

    String[] result = { "" };

    JButton okBtn = new JButton("OK");
    okBtn.addActionListener(e -> {
      result[0] = codeField.getText().trim();
      dialog.dispose();
    });
    codeField.addActionListener(e -> okBtn.doClick());
    // Closing the dialog with X also resolves to "" (app approval path)

    JPanel codeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    codeRow.add(new JLabel("Code (optional):"));
    codeRow.add(codeField);

    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
    buttons.add(okBtn);

    JPanel content = new JPanel();
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
    content.setBorder(BorderFactory.createEmptyBorder(14, 18, 10, 18));
    content.add(msgArea);
    content.add(Box.createVerticalStrut(12));
    content.add(codeRow);
    content.add(Box.createVerticalStrut(8));
    content.add(buttons);

    dialog.setContentPane(content);
    dialog.getRootPane().setDefaultButton(okBtn);
    // Fixed width so the message text wraps predictably
    dialog.setPreferredSize(new Dimension(420, dialog.getPreferredSize().height));
    dialog.pack();
    dialog.setLocationRelativeTo(null);
    dialog.setVisible(true);

    return result[0];
  }

  // -------------------------------------------------------------------------
  // EDT helpers
  // -------------------------------------------------------------------------

  private static void runOnEdt (Runnable r) {
    if (SwingUtilities.isEventDispatchThread()) {
      r.run();
      return;
    }
    try {
      SwingUtilities.invokeAndWait(r);
    } catch (Exception e) {
      System.err.println("[frenchpress] Swing dialog error: " + e);
    }
  }
}
