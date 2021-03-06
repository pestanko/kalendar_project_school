package cz.muni.fi.pv168.project.zaostan.gui.forms.users;

import cz.muni.fi.pv168.project.zaostan.gui.forms.models.UsersTableModel;
import cz.muni.fi.pv168.project.zaostan.kalendar.entities.User;
import cz.muni.fi.pv168.project.zaostan.kalendar.exceptions.user.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Pattern;

/**
 * Created by Pepo on 27.4.2015.
 */
public class UsersEditForm {
    private JLabel labelFirstName;
    private JLabel labelLastName;
    private JTextField textFirstName;
    private JTextField textLastName;
    private JLabel labelUserName;
    private JLabel labelEmail;
    private JLabel labelMobileNumber;
    private JLabel labelAddress;
    private JPanel mainPanel;
    private JTextField textUserName;
    private JTextField textEmail;
    private JTextField textMobileNumber;
    private JTextArea textAddress;
    private JButton btnSave;
    private JButton btnReset;
    private JButton btnClose;

    private UsersTableModel model;

    private JFrame frame;


    private User activeUser = null;

    final static Logger logger = LoggerFactory.getLogger(UsersEditForm.class);

    public UsersEditForm() {

    }

    public UsersEditForm(UsersTableModel model) {
        this.model = model;
        logger.debug("Model was set. " + model);


    }

    private boolean checkPattern(String text, String pattern)
    {
        Pattern reg = Pattern.compile(pattern);
        return reg.matcher(text).matches();
    }

    private boolean checkField(String name, String text, String pattern)
    {
        boolean b = checkPattern(text, pattern);
        if(!b)
        {
            JOptionPane.showMessageDialog( this.frame ,name + " is not valid");
        }
        return b;
    }

    public UsersEditForm(UsersTableModel model, User user)
    {
        this.model = model;
        this.activeUser = user;

        textEmail.setText(user.getEmail());
        textAddress.setText(user.getAddress());
        textFirstName.setText(user.getFirstName());
        textLastName.setText(user.getLastName());
        textUserName.setText(user.getUserName());
        textMobileNumber.setText(user.getMobileNumber());

    }

    public void showDialog() {

        SwingUtilities.invokeLater(() -> {
            frame = new JFrame();
            frame.setContentPane(this.mainPanel);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            initAllComponents();
            frame.pack();
            frame.setVisible(true);
        });


    }

    private void initAllComponents() {
        logger.debug("Initialize all components");

        btnClose.addActionListener(e -> {
            logger.debug("BtnClose was clicked. ");
            frame.dispose();
            frame.setVisible(false);
        });


        btnSave.addActionListener(e -> {

            if (activeUser == null) {
                addUserAction();
            } else
                editUserAction();


            frame.dispose();
            frame.setVisible(false);
        });


    }

    private void editUserAction() {


        boolean res = true;


        String username = textUserName.getText();
        //res &= checkField("User name", username, "");
        activeUser.setUserName(username);
        String firstname = textFirstName.getText();
        //res &= checkField("First name", firstname, "");

        activeUser.setFirstName(firstname);

        String lastname = textLastName.getText();
        //res &= checkField("Last name", lastname, "");

        activeUser.setLastName(lastname);
        String address = textAddress.getText();
        activeUser.setAddress(address);
        String mob_number = textMobileNumber.getText();
        activeUser.setMobileNumber(mob_number);
        String em = textEmail.getText();
        activeUser.setEmail(em);

        if(!res) return;

        try {
            model.updateUser(activeUser);
        } catch (UserException e) {
            /// LOGGER
            logger.error("Error while updating user in user edit form",e);
            e.printStackTrace();
        }


    }


    private void addUserAction() {
        try {
            logger.debug("Save was clicked.");
            User user = new User(textFirstName.getText(),
                    textLastName.getText(),
                    textUserName.getText(),
                    textEmail.getText());



            user.setAddress(textAddress.getText());
            user.setMobileNumber(textMobileNumber.getText());
            model.addUser(user);

        } catch (UserException e1) {
            e1.printStackTrace();
            logger.error("Error during adding user in UsersEditFom");
        }

    }


}
