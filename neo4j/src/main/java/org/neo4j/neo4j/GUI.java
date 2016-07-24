package org.neo4j.neo4j;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
 
/** Test JTextField, JPasswordField, JFormattedTextField, JTextArea */
@SuppressWarnings("serial")
public class GUI extends JFrame {
 
   // Private variables of the GUI components
   JTextField inputs;
   JTextField outputs;
   JTextArea tArea;
   JFormattedTextField formattedField;
 
   /** Constructor to set up all the GUI components */
   public GUI() {
      // JPanel for the text fields
      JPanel tfPanel = new JPanel(new GridLayout(3, 2, 10, 2));
      tfPanel.setBorder(BorderFactory.createTitledBorder("User Defined Task: "));
 
      // Regular text field (Row 1)
      tfPanel.add(new JLabel("  Inputs: "));
      inputs = new JTextField(100);
      tfPanel.add(inputs);
      inputs.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            tArea.append("\nYou have typed " + inputs.getText());
         }
      });
      tfPanel.add(new JLabel("  Outputs: "));
      outputs = new JTextField(100);
      tfPanel.add(outputs);
      outputs.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            tArea.append("\nYou have typed " + outputs.getText());
         }
      });
      JPanel LoadFilesPanel = new JPanel(new GridLayout(4, 2));
      LoadFilesPanel.setBorder(BorderFactory.createTitledBorder("Load Files: "));
 
      // Regular text field (Row 1)
      LoadFilesPanel.add(new JLabel("  Inputs: "));
      inputs = new JTextField(100);
      LoadFilesPanel.add(inputs);
      inputs.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            tArea.append("\nYou have typed " + inputs.getText());
         }
      });
      LoadFilesPanel.add(new JLabel("  Outputs: "));
      outputs = new JTextField(100);
      LoadFilesPanel.add(outputs);
      outputs.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            tArea.append("\nYou have typed " + outputs.getText());
         }
      });
 
      // Password field (Row 2)
//      tfPanel.add(new JLabel("  JPasswordField: "));
//      pwField = new JPasswordField(10);
//      tfPanel.add(pwField);
//      pwField.addActionListener(new ActionListener() {
//         @Override
//         public void actionPerformed(ActionEvent e) {
//            tArea.append("\nYou password is " + new String(pwField.getPassword()));
//         }
//      });
 
//      // Formatted text field (Row 3)
//      tfPanel.add(new JLabel("  JFormattedTextField"));
//      formattedField = new JFormattedTextField(java.util.Calendar
//            .getInstance().getTime());
//      tfPanel.add(formattedField);
 
      // Create a JTextArea
      tArea = new JTextArea("A JTextArea is a \"plain\" editable text component, "
            + "which means that although it can display text "
            + "in any font, all of the text is in the same font.");
      tArea.setFont(new Font("Serif", Font.ITALIC, 13));
      tArea.setLineWrap(true);       // wrap line
      tArea.setWrapStyleWord(true);  // wrap line at word boundary
      tArea.setBackground(new Color(204, 238, 241)); // light blue
      // Wrap the JTextArea inside a JScrollPane
      JScrollPane tAreaScrollPane = new JScrollPane(tArea);
      tAreaScrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      tAreaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
 
      // Setup the content-pane of JFrame in BorderLayout
      Container cp = this.getContentPane();
      cp.setLayout(new BorderLayout(5, 5));
      cp.add(tfPanel, BorderLayout.PAGE_START);
      cp.add(LoadFilesPanel, BorderLayout.CENTER);

      cp.add(tAreaScrollPane, BorderLayout.SOUTH);
 
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setTitle("Web services Composition");
      setSize(700, 700);
      setVisible(true);
   }
 
   /** The entry main() method */
   public static void main(String[] args) {
      // Run GUI codes in Event-Dispatching thread for thread safety
      SwingUtilities.invokeLater(new Runnable() {
         @Override
         public void run() {
            new GUI();  // Let the constructor do the job
         }
      });
   }
}
