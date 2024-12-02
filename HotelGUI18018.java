import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

class HotelGUI18018 extends JFrame {
    private Vector<Room> rooms = new Vector<>();
    private Connection connection;

    // Constructor
    public HotelGUI18018() {
        // Initialize database connection
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3307/serenity_suites", "root", "");
            initializeRooms();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database connection failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        // Set up main GUI layout
        setTitle("Serenity Suites Hotel Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLayout(new BorderLayout());

        // Attractive heading for hotel name
        JLabel hotelNameLabel = new JLabel("<html><span style='font-size:24px; color:blue;'>🏨 Serenity Suites Hotel 🏨</span></html>", JLabel.CENTER);
        hotelNameLabel.setFont(new Font("Serif", Font.BOLD, 28));
        hotelNameLabel.setForeground(Color.BLUE);
        add(hotelNameLabel, BorderLayout.NORTH);

        // Menu Panel
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new FlowLayout());

        JButton btnBookRoom = new JButton("Book Room");
        JButton btnViewAvailableRooms = new JButton("View Available Rooms");
        JButton btnListAllRooms = new JButton("List All Rooms");
        JButton btnExit = new JButton("Exit");

        menuPanel.add(btnBookRoom);
        menuPanel.add(btnViewAvailableRooms);
        menuPanel.add(btnListAllRooms);
        menuPanel.add(btnExit);

        add(menuPanel, BorderLayout.NORTH);

        // Main Display Panel
        JTextArea displayArea = new JTextArea();
        displayArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(displayArea);
        add(scrollPane, BorderLayout.CENTER);

        // Button Actions
        btnBookRoom.addActionListener(e -> bookRoom(displayArea));
        btnViewAvailableRooms.addActionListener(e -> viewAvailableRooms(displayArea));
        btnListAllRooms.addActionListener(e -> listAllRooms(displayArea));
        btnExit.addActionListener(e -> System.exit(0)); // Exit the application

        setVisible(true);
    }

    private void initializeRooms() {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM rooms");
            while (rs.next()) {
                int roomNumber = rs.getInt("room_number");
                String type = rs.getString("type");
                boolean available = rs.getBoolean("available");
                Room room = new Room(roomNumber, type);
                room.setAvailable(available);
                rooms.add(room);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading rooms: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void bookRoom(JTextArea displayArea) {
        JDialog bookingDialog = new JDialog(this, "Book Room", true);
        bookingDialog.setSize(400, 350);
        bookingDialog.setLayout(new GridLayout(6, 2));

        // Form Inputs
        JTextField txtName = new JTextField();
        JTextField txtContact = new JTextField();
        JTextField txtAddress = new JTextField();
        JTextField txtEmail = new JTextField();
        JComboBox<String> roomSelection = new JComboBox<>();

        // Populate available rooms
        for (Room room : rooms) {
            if (room.isAvailable()) {
                roomSelection.addItem("Room " + room.getRoomNumber() + " (" + room.getType() + ")");
            }
        }

        JButton btnBook = new JButton("Book");
        JButton btnCancel = new JButton("Cancel");

        bookingDialog.add(new JLabel("Name:"));
        bookingDialog.add(txtName);
        bookingDialog.add(new JLabel("Contact:"));
        bookingDialog.add(txtContact);
        bookingDialog.add(new JLabel("Address:"));
        bookingDialog.add(txtAddress);
        bookingDialog.add(new JLabel("Email:"));
        bookingDialog.add(txtEmail);
        bookingDialog.add(new JLabel("Select Room:"));
        bookingDialog.add(roomSelection);
        bookingDialog.add(btnBook);
        bookingDialog.add(btnCancel);

        btnCancel.addActionListener(e -> bookingDialog.dispose());
        btnBook.addActionListener(e -> {
            String name = txtName.getText().trim();
            String contact = txtContact.getText().trim();
            String address = txtAddress.getText().trim();
            String email = txtEmail.getText().trim();
            String selectedRoom = (String) roomSelection.getSelectedItem();

            // Perform validations
            if (!validateName(name)) {
                JOptionPane.showMessageDialog(bookingDialog, "Invalid name. Only letters and spaces are allowed.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!validateContact(contact)) {
                JOptionPane.showMessageDialog(bookingDialog, "Invalid contact. Enter a valid 10-digit number.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!validateAddress(address)) {
                JOptionPane.showMessageDialog(bookingDialog, "Invalid address. It must be at least 5 characters long.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!validateEmail(email)) {
                JOptionPane.showMessageDialog(bookingDialog, "Invalid email. Enter a valid email address.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (selectedRoom == null) {
                JOptionPane.showMessageDialog(bookingDialog, "No room selected. Please choose a room.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Book room
            Room room = null;
            for (Room r : rooms) {
                if (selectedRoom.contains("Room " + r.getRoomNumber())) {
                    room = r;
                    break;
                }
            }

            if (room != null) {
                room.setAvailable(false);
                updateRoomAvailability(room.getRoomNumber(), false);
                saveBooking(room, new Customer(name, contact, address, email));
                displayArea.append("Room " + room.getRoomNumber() + " booked successfully for " + name + ".\n");
                bookingDialog.dispose();
            } else {
                JOptionPane.showMessageDialog(bookingDialog, "Room booking failed. Try again.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        bookingDialog.setVisible(true);
    }

    private boolean validateName(String name) {
        return name.matches("[A-Za-z ]+");
    }

    private boolean validateContact(String contact) {
        return contact.matches("\\d{10}");
    }

    private boolean validateAddress(String address) {
        return address.length() >= 5;
    }

    private boolean validateEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private void updateRoomAvailability(int roomNumber, boolean available) {
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE rooms SET available = ? WHERE room_number = ?")) {
            stmt.setBoolean(1, available);
            stmt.setInt(2, roomNumber);
            stmt.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating room availability: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveBooking(Room room, Customer customer) {
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO bookings (customer_name, contact, address, email, rooms) VALUES (?, ?, ?, ?, ?)")) {
            stmt.setString(1, customer.getName());
            stmt.setString(2, customer.getContact());
            stmt.setString(3, customer.getAddress());
            stmt.setString(4, customer.getEmail());
            stmt.setString(5, String.valueOf(room.getRoomNumber()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error saving booking: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewAvailableRooms(JTextArea displayArea) {
        displayArea.setText("Available Rooms:\n");
        for (Room room : rooms) {
            if (room.isAvailable()) {
                displayArea.append(room + "\n");
            }
        }
    }

    private void listAllRooms(JTextArea displayArea) {
        displayArea.setText("All Rooms:\n");
        for (Room room : rooms) {
            displayArea.append(room + "\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HotelGUI18018::new);
    }
}
