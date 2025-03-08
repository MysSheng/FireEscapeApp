package com.app.myapp;

public class Main {
    public static void main(String[] args) {
        // 初步建構一個大室內空間
        Grid[][] grid = new Grid[84][66];
        int user_x = 9, user_y = 45;
        int fire_x = 27, fire_y = 0;

        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                grid[i][j] = new Grid(i, j);
            }
        }

        for (int i = 0; i < 72; i++) {
            grid[i][0].setType(Grid.WALL);
            grid[i][24].setType(Grid.WALL);
            grid[i][39].setType(Grid.WALL);
            grid[i][63].setType(Grid.WALL);
        }

        for (int i = 0; i < 27; i++) {
            grid[0][i].setType(Grid.WALL);
            grid[24][i].setType(Grid.WALL);
            grid[48][i].setType(Grid.WALL);
            grid[69][i].setType(Grid.WALL);
        }

        for (int i = 39; i < 66; i++) {
            grid[0][i].setType(Grid.WALL);
            grid[24][i].setType(Grid.WALL);
            grid[48][i].setType(Grid.WALL);
            grid[69][i].setType(Grid.WALL);
        }

        for (int i = 51; i < 69; i++) {
            grid[i][24].setType(Grid.ROAD);
            grid[i][39].setType(Grid.ROAD);
        }

        grid[69][27].setType(Grid.WALL);
        grid[69][36].setType(Grid.WALL);
        grid[63][27].setType(Grid.WALL);
        grid[66][27].setType(Grid.WALL);
        grid[63][36].setType(Grid.WALL);
        grid[66][36].setType(Grid.WALL);

        // 出口跟火源
        grid[69][30].setType(Grid.EXIT);
        grid[48][27].setType(Grid.EXIT);
        grid[48][36].setType(Grid.EXIT);
        grid[18][24].setType(Grid.EXIT);
        grid[18][39].setType(Grid.EXIT);
        grid[30][24].setType(Grid.EXIT);
        grid[30][39].setType(Grid.EXIT);
        grid[0][30].setType(Grid.EXIT);
        grid[24][12].setType(Grid.EXIT);

        grid[27][0].setType(Grid.FIRE);

        Planner fp = new Planner(grid, user_x, user_y, fire_x, fire_y);
        fp.addRoom(new Room(1, 0, 0, 27, 27));
        fp.addRoom(new Room(2, 24, 0, 27, 27));
        fp.addRoom(new Room(3, 48, 0, 24, 36));
        fp.addRoom(new Room(4, 0, 24, 51, 18));
        fp.addRoom(new Room(5, 0, 39, 27, 27));
        fp.addRoom(new Room(6, 24, 39, 27, 27));
        fp.addRoom(new Room(7, 48, 30, 24, 36));

        fp.setRunSpeed(2);
        fp.do_task();
        fp.testNavigator();
        // fp.testEdge();
        fp.testOutgoingEdge();
        // fp.user_guide(user_x, user_y);

    }

    // 將 direction 數字轉換為文字表示
    private static String getDirectionText(int direction) {
        switch (direction) {
            case Grid.UP:
                return "↑";
            case Grid.DOWN:
                return "↓";
            case Grid.LEFT:
                return "←";
            case Grid.RIGHT:
                return "→";
            case Grid.UP_LEFT:
                return "↖";
            case Grid.UP_RIGHT:
                return "↗";
            case Grid.DOWN_LEFT:
                return "↙";
            case Grid.DOWN_RIGHT:
                return "↘";
            default:
                return "x"; // 無方向
        }
    }
}