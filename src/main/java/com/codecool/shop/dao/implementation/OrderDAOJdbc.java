package com.codecool.shop.dao.implementation;

import com.codecool.shop.dao.OrderDAO;
import com.codecool.shop.model.Cart;
import com.codecool.shop.model.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class OrderDAOJdbc implements OrderDAO {
    private static OrderDAOJdbc instance = null;
    private PSQLConnection psqlConnection;

    public static OrderDAOJdbc getInstance(){
        if (instance == null){
            instance = new OrderDAOJdbc();
        }
        return instance;
    }

    public boolean addNewOrder(Cart cart){
       List<Item> orderedItems =  cart.getShoppingCart();
        psqlConnection = PSQLConnection.getInstance();
        int generatedUserId = 0;
        int generatedOrderId = 0;

        // create new user in DB, this is an anonim user atm.
        // TODO should be extracted to different DAO (eg.User)
        String sqlUser = "INSERT INTO users (status) VALUES(?) RETURNING user_id";
        String sqlOrder = "INSERT INTO orders (user_id, payment_method, status, order_time) " +
                    "VALUES (?, ?, ?, ?)";

        String sqlOrderedItems = "INSERT INTO ordered_items (order_id, product_id, quantity) " +
                                 "VALUES (?, ?, ?)";

        try (Connection conn = psqlConnection.getConnection()){
            PreparedStatement pstmt = conn.prepareStatement(sqlUser);
            // TODO set status from enum
            pstmt.setString(1, "default");
            pstmt.execute();
            ResultSet insertedUser = pstmt.getResultSet();
            while (insertedUser.next()) {
                generatedUserId = insertedUser.getInt("user_id");
                System.out.println(generatedUserId);
            }
            insertedUser.close();


            // create new order in DB
            pstmt = conn.prepareStatement(sqlOrder);
            pstmt.setInt(1, generatedUserId);
            pstmt.setString(2, "cash");
            pstmt.setString(3, "new");
            // java.sql.date is a java.util.date as well. Other way, is not. Need to convert.
            pstmt.setDate(4, new java.sql.Date( (new java.util.Date()).getTime() ));
            pstmt.execute();
            // TODO the error report points to this line. Resultset is nullpointer.
            /* I ran into the same issue and I discovered that if other thread uses the same connection,
            the ResultSet will throw a NullPointerException after the other sql is executed

             */
            ResultSet insertedOrder = pstmt.getResultSet();
            if (insertedOrder == null){
                System.out.println("it is null!");
                return false;
            }
            while (insertedOrder.next()) {
                generatedOrderId = insertedOrder.getInt("order_id");
                System.out.println(generatedOrderId);
            }
            insertedOrder.close();

            // fill the new ordered_items in DB
            pstmt = conn.prepareStatement(sqlOrderedItems);
            // there could be multiple items in a single order.
            for (Item orderedItem : orderedItems) {
                pstmt.setInt(1, generatedOrderId);
                pstmt.setInt(2, orderedItem.getProduct().getId());
                pstmt.setInt(3, orderedItem.getQuantity());
                pstmt.execute();
            }



        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}