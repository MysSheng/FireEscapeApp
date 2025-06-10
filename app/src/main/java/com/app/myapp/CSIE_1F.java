package com.app.myapp;

public class CSIE_1F {
    private final Grid[][] grid = new Grid[100][100];

    public Grid[][] getGrid() {
        return grid;
    }

    public CSIE_1F() {
        // 初始化每個grid(設為wall)
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                grid[i][j] = new Grid(i, j);
                grid[i][j].setType(Grid.WALL);
            }
        }
        setRoadArea(27, 31, 2, 17);
        setRoadArea(12, 26, 16, 18);
        //setRoadLines(12, 26, new int[]{16, 17, 18}, false);
        setRoadArea(14, 24, 12, 14);
        //setRoadLines(14, 24, new int[]{12, 13, 14}, false);
        setRoadLine(23, 26, 15, true);
        setRoad(25, 14);
        setRoad(26, 14);
        setRoadArea(32, 37, 11, 28);
        for (int i = 11; i <= 28; i++) {
            setRoad(38, i + 1);
            setRoad(39, i + 2);
            setRoad(40, i + 3);
        }
        setWall(40, 16);
        setWall(40, 24);
        setWall(39, 24);
        setRoadArea(12, 20, 20, 25);
        setRoad(21, 25);
        setRoadArea(27, 31, 18, 26);
        setWall(27, 18);
        setWall(28, 18);
        setWall(29, 22);
        setWall(27, 23);
        setWall(28, 23);
        setWall(27, 26);
        setRoadArea(22, 26, 20, 26);
        setWall(24, 20);
        setWall(25, 21);
        setWall(25, 22);
        setWall(26, 23);
        setWall(26, 24);
        setWall(26, 25);
        setWall(22, 25);
        setRoadArea(14, 22, 26, 30);
        setRoad(15, 31);
        setWall(14, 27);
        setWall(15, 27);
        setWall(16, 27);
        setWall(17, 26);
        setWall(18, 26);
        setWall(19, 26);
        setWall(19, 27);
        setWall(20, 28);
        setWall(19, 30);
        setWall(20, 30);
        setWall(21, 30);
        setRoadArea(23, 33, 28, 31);
        setWall(23, 31);
        setWall(29, 31);
        setWall(30, 31);
        setWall(32, 31);
        setWall(32, 30);
        setRoadArea(17, 22, 31, 37);
        setWall(17, 31);
        setWall(18, 31);
        setWall(22, 31);
        setWall(17, 34);
        setWall(20, 37);
        setWall(21, 37);
        setWall(22, 37);
        setRoad(16, 32);
        setRoad(16, 33);
        setRoad(18, 38);
        setRoadArea(23, 25, 32, 35);
        setRoadLine(32, 34, 27, false);
        setRoadArea(29, 32, 32, 33);
        setWall(32, 33);
        setRoadArea(34, 37, 30, 31);
        setWall(36, 31);
        setWall(37, 31);
        setRoadArea(41, 45, 21, 22);
        setRoad(41, 23);
        setRoad(43, 23);
        setRoad(42, 24);
        setRoadArea(39, 43, 25, 34);
        setWall(39, 34);
        setWall(27, 19);
        setWall(24, 21);
        setWall(25, 23);
        setWall(29, 23);
        setWall(26, 26);
        setRoad(27, 26);  // 明確標註為 ROAD，避免被牆蓋掉
        setWall(22, 24);
        setWall(16, 26);
        setWall(19, 28);
        setWall(15, 31);
        setWall(19, 31);
        setWall(21, 31);
        setWall(28, 31);
        setWall(33, 31);
        setWall(35, 31);
        setWall(42, 24);
        setRoad(42, 23);
        setRoadArea(33, 38, 32, 34);
        setWall(33, 32);
        setWall(34, 32);
        setWall(35, 32);
        setWall(33, 33);
        setWall(38, 34);
        setRoadArea(29, 38, 35, 45);
        setRoadArea(23, 28, 37, 49);
        setWall(23, 37);
        setWall(28, 49);
        setRoadLine(38, 46, 22, false); // 橫向列：22列（第23行~第47行）
        setRoadLine(38, 43, 21, false); // 橫向列：21列（第23行~第44行）
        setRoad(20, 39);
        setRoad(20, 40);
        for (int i = 22; i <= 24; i++) {
            setRoad(i, 50);
        }
        setRoadArea(29, 33, 46, 47);
        setRoad(29, 48);
        setRoad(30, 48);
        for (int i = 34; i <= 36; i++) {
            setRoad(i, 46);
        }
        setRoadLine(36, 45, 39, false);
        setRoadLine(40, 44, 40, false);
        setRoadLine(43, 44, 41, false);
        setRoadLine(35, 38, 41, false);
        setRoadArea(42, 45, 28, 47);
        setWall(45, 28);  // 區塊入口處加牆
        setRoadLine(28, 30, 46, false);  // 第46列
        setRoadLine(28, 29, 47, false);  // 第47列
        setRoadArea(38, 42, 47, 59);
        setRoadArea(35, 39, 48, 60);
        setRoadArea(32, 37, 49, 61);
        setRoadArea(29, 33, 50, 62);
        setRoadArea(28, 30, 51, 63);
        setRoadArea(43, 44, 49, 58);
        setRoadArea(26, 27, 51, 60);          // 橫向長廊
        setRoadArea(23, 24, 52, 53);          // 上方連接支路
        setRoadLine(48, 49, 45, false);       // 第45列，48~49
        setRoadLine(54, 58, 45, false);       // 第45列，54~58
        setRoadLine(34, 52, 46, false);       // 第46列橫向長通道
        setRoadLine(57, 58, 46, false);       // 第46列補接尾端
        setRoadLine(52, 55, 25, false);       // 第25列短線段
        setRoad(27, 64);
        setRoad(28, 64);
        setRoadLine(37, 39, 47, false);       // 第47列小線段
        setRoadArea(47, 49, 35, 36);          // 小區塊
        setRoadArea(47, 48, 40, 41);          // 小區塊
        setRoadArea(51, 52, 41, 42);          // 小區塊
        setRoadArea(47, 50, 42, 47);          // 向右延伸區塊
        setWall(50, 44);                      // 補牆
        setRoadArea(47, 54, 48, 49);          // 左上角通道
        setRoadArea(47, 52, 50, 53);          // 中段通道
        setWall(52, 50);                      // 中段補牆
        setRoadArea(47, 54, 54, 59);          // 右段主通道
        setWall(54, 54);                      // 右段補牆
        setRoadArea(44, 55, 60, 61);
        setRoadArea(55, 56, 54, 55);
        setRoad(41, 61);
        setRoad(42, 61);
        setRoad(43, 61);
        setRoad(41, 46);
        setWall(44, 49);
        setWall(44, 50);
        setRoadArea(39, 59, 62, 67);
        setRoadArea(60, 64, 65, 67);
        setRoadArea(35, 38, 63, 67);
        setRoadArea(32, 34, 64, 67);
        setRoadArea(30, 32, 65, 68);
        setRoadArea(29, 30, 66, 69);
        setRoad(58, 61);
        setRoad(59, 61);
        setRoad(28, 66);
        setRoad(60, 64);
        setWall(38, 63);
        setWall(40, 67);
        setRoadArea(48, 64, 68, 86); // 底層通道
        setRoadArea(34, 40, 69, 87); // 中段
        setRoadArea(32, 42, 70, 85); // 擴張上方
        setRoadArea(29, 43, 71, 84); // 更上
        setRoadArea(27, 44, 72, 83); // 往上
        setRoadArea(25, 46, 73, 82); // 最上範圍
        setRoadLine(42, 47, 68, true);
        setRoadLine(44, 47, 69, true);
        setRoadLine(45, 47, 70, true);
        setRoadLine(46, 47, 71, true);
        setRoad(47, 72); // 最底接點
        setRoad(46, 84);
        setRoadLine(45, 46, 85, true);
        setRoadLine(44, 46, 86, true);
        setRoadLine(42, 44, 87, true);
        setRoadLine(40, 42, 88, true);
        setRoadLine(36, 40, 89, true);
        setRoadLine(50, 61, 87, true); // 下方連接（右下區）
        setRoadLine(73, 76, 47, false);
        setRoadLine(82, 86, 47, false);
        setRoadLine(90, 91, 36, false); // 最上方橫向
        setRoadLine(80, 86, 65, false); // 最下方連結段
        setWall(46, 81);
        setWall(46, 82);
        setWall(39, 87);
        setWall(40, 87);
        setRoadLine(20, 34, 88, true);        // 直向主路
        setRoadArea(17, 33, 82, 87);          // 長方形主走廊
        setWall(17, 87);                      // 上方封牆
        for (int i = 17; i <= 24; i++) {
            for (int j = 0; j >= 17 - i; j--) {
                setRoad(i, 81 + j);           // 斜線段（81,82,83...）階梯狀上升
            }
        }
        setRoadArea(46, 51, 88, 89);// 最右角擴展通道
        setRoadArea(49, 51, 88, 94);     // 底層走道
        setRoadArea(44, 46, 89, 90);     // 向上延伸
        setRoadArea(42, 44, 90, 91);     // 更上層
        setRoadArea(38, 41, 91, 92);     // 最上層通道
        setRoadArea(46, 48, 92, 93);     // 右側支線
        setRoadArea(41, 47, 94, 96);     // 頂部擴展
        setRoadLine(43, 45, 93, true);   // 中央縱向通道
        setRoadLine(50, 51, 95, true);   // 右上小段
        setRoad(40, 94);                 // 單點連接
        setRoad(48, 91);
        setRoad(48, 94);
        setRoad(52, 88);                 // 最外延點
        setRoadArea(65, 67, 67, 74);     // 左起步寬區
        setRoadArea(65, 93, 68, 71);     // 向下延伸長條區
        setRoadArea(71, 72, 65, 67);
        setRoadArea(79, 80, 64, 67);
        setRoadLine(81, 93, 67, true);   // 一直往下
        setRoadArea(92, 93, 67, 73);     // 最下段展開
        setRoadArea(91, 95, 71, 72);     // 向下平行段
        setRoadLine(68, 83, 72, true);   // 向上通道
        setRoadLine(77, 82, 73, true);   // 平行支線
        setRoadArea(66, 66, 75, 78);     // 起始方塊
        setRoadArea(67, 79, 75, 89);     // 大面積展開
        setRoadLine(70, 75, 74, true);   // 左側進入口
        setRoadLine(78, 79, 74, true);   // 向右的通道開口（從左下）
        setRoadArea(80, 80, 85, 90);     // 往右的房間右下角
        setRoadArea(80, 81, 90, 90);     // 最右邊的通道
        setRoadArea(81, 81, 74, 83);     // 整條水平走道
        setRoadArea(82, 96, 74, 87);     // 整個東南房區展開
        setRoadLine(85, 89, 73, true);   // 向左的支線（可能為逃生出口？）
        setRoadLine(82, 94, 88, true);   // 東邊垂直通道
        setRoadArea(97, 97, 73, 80);     // 最下層邊界橫向出口（或另一個房間）
        setRoad(68, 73);                 // 左上方起點連接（與東區相連）
        //額外補正點
        grid[65][67].setType(Grid.WALL);
        grid[66][67].setType(Grid.WALL);
        grid[67][67].setType(Grid.WALL);
        grid[65][68].setType(Grid.WALL);
        grid[65][67].setType(Grid.WALL);
        grid[67][68].setType(Grid.WALL);
        grid[68][68].setType(Grid.WALL);
        grid[69][68].setType(Grid.WALL);
        grid[70][68].setType(Grid.WALL);
        grid[80][64].setType(Grid.WALL);
        grid[80][65].setType(Grid.WALL);
        grid[80][66].setType(Grid.WALL);
        grid[80][67].setType(Grid.WALL);
        grid[81][67].setType(Grid.WALL);
        grid[82][67].setType(Grid.WALL);
        grid[83][67].setType(Grid.WALL);
        grid[84][67].setType(Grid.WALL);
        grid[85][67].setType(Grid.WALL);
        grid[60][64].setType(Grid.WALL);
        grid[60][65].setType(Grid.WALL);
        grid[61][65].setType(Grid.WALL);
        grid[62][65].setType(Grid.WALL);
        grid[63][65].setType(Grid.WALL);
        grid[64][65].setType(Grid.WALL);
        grid[58][61].setType(Grid.WALL);
        grid[59][61].setType(Grid.WALL);
        grid[59][62].setType(Grid.WALL);
        grid[59][63].setType(Grid.WALL);
        grid[55][60].setType(Grid.WALL);
        grid[55][61].setType(Grid.WALL);
        grid[54][56].setType(Grid.WALL);
        grid[54][57].setType(Grid.WALL);
        grid[54][58].setType(Grid.WALL);
        grid[53][54].setType(Grid.WALL);
        grid[52][51].setType(Grid.WALL);
        grid[52][52].setType(Grid.WALL);
        grid[51][48].setType(Grid.WALL);
        grid[50][45].setType(Grid.WALL);
        grid[50][46].setType(Grid.WALL);
        grid[49][43].setType(Grid.WALL);
        grid[49][43].setType(Grid.WALL);
        grid[48][40].setType(Grid.WALL);
        grid[47][37].setType(Grid.WALL);
        grid[47][38].setType(Grid.WALL);
        grid[46][34].setType(Grid.WALL);
        grid[46][35].setType(Grid.WALL);
        grid[47][35].setType(Grid.WALL);
        grid[45][30].setType(Grid.WALL);
        grid[45][31].setType(Grid.WALL);
        grid[45][32].setType(Grid.WALL);
        grid[44][28].setType(Grid.WALL);
        grid[43][25].setType(Grid.WALL);
        grid[43][26].setType(Grid.WALL);
        for(int i=2;i<=11;i++){
            grid[27][i].setType(Grid.WALL);
        }

        // 設置火點
        grid[24][45].setType(Grid.FIRE);
        // 設置出口
        grid[29][1].setType(Grid.EXIT);
        grid[28][68].setType(Grid.EXIT);
        grid[55][58].setType(Grid.EXIT);
        grid[58][88].setType(Grid.EXIT);
        grid[90][65].setType(Grid.EXIT);

        grid[28][1].setType(Grid.WALL);
        grid[55][59].setType(Grid.WALL);
        grid[90][66].setType(Grid.ROAD);
    }

    // 輔助方法：設置矩形區域為ROAD
    private void setRoadArea(int startX, int endX, int startY, int endY) {
        for (int i = startX; i <= endX; i++) {
            for (int j = startY; j <= endY; j++) {
                grid[i][j].setType(Grid.ROAD);
            }
        }
    }

    // 輔助方法：設置水平或垂直線為ROAD
    private void setRoadLine(int start, int end, int constant, boolean isVertical) {
        if (isVertical) {
            for (int i = start; i <= end; i++) {
                grid[i][constant].setType(Grid.ROAD);
            }
        } else {
            for (int j = start; j <= end; j++) {
                grid[constant][j].setType(Grid.ROAD);
            }
        }
    }

    // 輔助方法：設置單個格子為WALL
    private void setWall(int x, int y) {
        grid[x][y].setType(Grid.WALL);
    }

    // 輔助方法：設置單個格子為ROAD
    private void setRoad(int x, int y) {
        grid[x][y].setType(Grid.ROAD);
    }

}
