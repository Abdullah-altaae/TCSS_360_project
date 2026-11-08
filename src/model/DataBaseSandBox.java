package model;

import java.sql.SQLException;

public class DataBaseSandBox {

    private DataBaseSandBox() {

    }

    public static void main(final String[] args) throws SQLException {
        final RouteDatabase db = new RouteDatabase("jdbc:sqlite:CarPlanner_DataBase.db");
        db.createTables();

    }
}
