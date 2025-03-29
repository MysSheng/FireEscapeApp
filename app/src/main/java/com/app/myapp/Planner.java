package com.app.myapp;

import java.util.*;
import java.util.Map.Entry;

public class Planner {
    private Grid[][] GridMap;
    private final int ROWS;
    private final int COLS;
    private static Room[] rooms = new Room[100];
    private static int rooms_count =0;
    private final int user_loc_x;
    private final int user_loc_y;
    private final int fire_loc_x;
    private final int fire_loc_y;
    private double runSpeed =0;
    private ArrayList<Edge> edges = new ArrayList<>();

    private HashMap<Grid,Grid[][]> global_nav = new HashMap<>();

    public Planner(Grid[][] GridMap,int user_loc_x, int user_loc_y,
                   int fire_loc_x,int fire_loc_y) {
        this.GridMap = GridMap;
        this.ROWS = GridMap.length;
        this.COLS = GridMap[0].length;
        this.user_loc_x = user_loc_x;
        this.user_loc_y = user_loc_y;
        this.fire_loc_x = fire_loc_x;
        this.fire_loc_y = fire_loc_y;
    }

    public void addRoom(Room room) {
        rooms[rooms_count+1] = room;
        rooms_count++;
    }

    public void setRunSpeed(double runSpeed) {
        this.runSpeed = runSpeed;
    }

    public void do_task(){
        count_fire_time();
        divide_floor_plane();
        count_local_path();
        init_high_level_map();
        concise_high_level_map();
    }

    public void do_one_level(){
        count_fire_time();
        count_global_path();
    }

    public void count_global_path(){
        Queue<Grid> exits = new LinkedList<Grid>();
        //collect exits
        exits.offer(GridMap[0][10]);
        exits.offer(GridMap[23][10]);
        //starting compute
        HashMap<Grid,Grid[][]> navigator = new HashMap<>();
        while (!exits.isEmpty()) {
            Grid exit = exits.poll();
            //set Target Node.deadline as Target Node.fireTime
            GridMap[exit.getX()][exit.getY()].setDeadLine(exit.getFireTime());
            //Create empty queue
            Queue<Grid> relax_list = new LinkedList<Grid>();
            //push Target Node into queue relax_list
            relax_list.add(GridMap[exit.getX()][exit.getY()]);
            //while relax_list is not empty
            while (!relax_list.isEmpty()) {
                //pop out the first element 'front' from 'relax_list'
                Grid front = relax_list.remove();
                //for each grid in front's neighbor grids (from (x-1,y-1) to (x+1,y+1)
                Queue<Grid> neighbors = new LinkedList<Grid>();
                int x=front.getX(), y=front.getY();
                for(int i=x-1;i<=x+1;i++){
                    for(int j=y-1;j<=y+1;j++){
                        //check if the index is overflow
                        if(i >=0 && i < ROWS && j >= 0 && j < COLS){
                            int type=GridMap[i][j].getType();
                            if(type==Grid.ROAD||type==Grid.EXIT) {
                                neighbors.add(GridMap[i][j]);
                            }
                        }
                    }
                }
                relax(front,relax_list,neighbors,GridMap, 0, 0);
            }
            //use an independent array to store the value of localMap now
            //deep copy
            Grid temp=new Grid(GridMap[exit.getX()][exit.getY()]);
            Grid[][] copy= new Grid[ROWS][COLS];
            System.out.println("("+exit.getX()+","+exit.getY()+")");
            //test code
            for(int i=0;i<ROWS;i++){
                for(int j=0;j<COLS;j++) {
                    //localMap[i][j].setDeadLine(localMap[i][j].getFireTime());
                    copy[i][j] = new Grid(GridMap[i][j]);
                    double num = GridMap[i][j].getDeadLine();
                    String formattedNum = String.format("%.2f", num);
                    System.out.print(formattedNum+" ");
                }
                System.out.println();
            }
            System.out.println();
            navigator.put(temp, copy);
        }
        global_nav = navigator;
    }

    /* step0:
     * Initialize the fire arrival time for each grid
     */

    public void count_fire_time(){
        for(int i=0;i<ROWS;i++)
            for(int j=0;j<COLS;j++)
                if(GridMap[i][j].getType()==Grid.ROAD||GridMap[i][j].getType()==Grid.EXIT)
                    GridMap[i][j].setFireTime(fire_loc_x,fire_loc_y);
    }

    /* step1:
     * Divide the floor plane into numeral blocks
     */

    public void divide_floor_plane(){
        for(int r=1;r<=rooms_count;r++){
            int rows = rooms[r].getRows() , cols = rooms[r].getCols();
            int pivot_x = rooms[r].getPivot_X() , pivot_y = rooms[r].getPivot_Y();
            Grid[][] local_gridMap = new Grid[rows][cols];
            for(int i=0;i<rows;i++)
                for(int j=0;j<cols;j++)
                    local_gridMap[i][j] = GridMap[pivot_x+i][pivot_y+j];
            rooms[r].setGridMap(local_gridMap);
        }
    }

    /* step2:
     * count the local path for each local exit
     * for each room
     */

    public void count_local_path() {
        for (int r = 1; r <= rooms_count; r++) {
            Room room = rooms[r];
            Queue<Grid> exits = new LinkedList<Grid>();
            Grid[][] localMap = room.getGridMap();
            //collect exits
            int rows = room.getRows(), cols = room.getCols();
            for (int i = 0; i < rows; i++)
                for (int j = 0; j < cols; j++)
                    if (localMap[i][j].getType() == Grid.EXIT)
                        exits.offer(localMap[i][j]);
            //starting compute
            HashMap<Grid,Grid[][]> navigator = new HashMap<>();
            while (!exits.isEmpty()) {
                Grid exit = exits.poll();
                //set Target Node.deadline as Target Node.fireTime
                int local_X=exit.getX()-room.getPivot_X(), local_Y=exit.getY()-room.getPivot_Y();
                localMap[local_X][local_Y].setDeadLine(exit.getFireTime());;
                //Create empty queue
                Queue<Grid> relax_list = new LinkedList<Grid>();
                //push Target Node into queue relax_list
                relax_list.add(localMap[local_X][local_Y]);
                //while relax_list is not empty
                while (!relax_list.isEmpty()) {
                    //pop out the first element 'front' from 'relax_list'
                    Grid front = relax_list.remove();
                    //for each grid in front's neighbor grids (from (x-1,y-1) to (x+1,y+1)
                    Queue<Grid> neighbors = new LinkedList<Grid>();
                    int x=front.getX(), y=front.getY();
                    for(int i=x-1;i<=x+1;i++){
                        for(int j=y-1;j<=y+1;j++){
                            //check if the index is overflow
                            int bios_X=i-room.getPivot_X(), bios_Y=j-room.getPivot_Y();
                            if(bios_X >=0 && bios_X < room.getRows() && bios_Y >= 0 && bios_Y < room.getCols()){
                                int type=localMap[bios_X][bios_Y].getType();
                                if(type==Grid.ROAD||type==Grid.EXIT) {
                                    neighbors.add(localMap[bios_X][bios_Y]);
                                }
                            }
                        }
                    }
                    relax(front,relax_list,neighbors,localMap, room.getPivot_X(), room.getPivot_Y());
                }
                //use an independent array to store the value of localMap now
                //deep copy
                Grid temp=new Grid(localMap[local_X][local_Y]);
                Grid[][] copy= new Grid[rows][cols];
                System.out.println("("+exit.getX()+","+exit.getY()+")");
                //test code
                for(int i=0;i<rows;i++){
                    for(int j=0;j<cols;j++) {
                        //localMap[i][j].setDeadLine(localMap[i][j].getFireTime());
                        copy[i][j] = new Grid(localMap[i][j]);
                        double num = localMap[i][j].getDeadLine();
                        String formattedNum = String.format("%.2f", num);
                        System.out.print(formattedNum+" ");
                    }
                    System.out.println();
                }
                System.out.println();
                navigator.put(temp, copy);
                //set up for next loop
                for(int i=0;i<room.getRows();i++)
                    for(int j=0;j<room.getCols();j++) {
                        localMap[i][j].setDirection(-1);
                        localMap[i][j].setDeadLine(0);
                    }
            }
            //set the navigator for room
            room.setNavigators(navigator);
        }
    }

    public void relax(Grid front,Queue<Grid> list,Queue<Grid> neighbors,Grid[][] localMap,int pivot_X,int pivot_Y) {
        //do eight directions of front
        while(!neighbors.isEmpty()) {
            Grid grid = neighbors.poll();
            int frontX=front.getX(), frontY=front.getY();
            int gridX=grid.getX(), gridY=grid.getY();
            //set newDeadLine as Min(front.deadline-TravelingTime(front,grid),grid.fireTime)
            //suppose people's speed is 2m/s amd the grid's length is 1m
            double newDeadLine=Math.min(front.getDeadLine()-1/runSpeed,grid.getFireTime());
            if(Math.abs(frontX-gridX)+Math.abs(frontY-gridY)==2) newDeadLine=Math.min(front.getDeadLine()-1/runSpeed*Math.sqrt(2),grid.getFireTime());
            if(newDeadLine > grid.getDeadLine()){
                grid.setDeadLine(newDeadLine);
                int direction=-1;
                if(gridX == frontX - 1 && gridY == frontY - 1) direction = Grid.DOWN_RIGHT;
                if(gridX == frontX - 1 && gridY == frontY) direction = Grid.DOWN;
                if(gridX == frontX - 1 && gridY == frontY + 1) direction = Grid.DOWN_LEFT;
                if(gridX == frontX && gridY == frontY - 1) direction = Grid.RIGHT;
                if(gridX == frontX && gridY == frontY + 1) direction = Grid.LEFT;
                if(gridX == frontX + 1 && gridY == frontY - 1) direction = Grid.UP_RIGHT;
                if(gridX == frontX + 1 && gridY == frontY) direction = Grid.UP;
                if(gridX == frontX + 1 && gridY == frontY + 1) direction = Grid.UP_LEFT;
                grid.setDirection(direction);
                //if grid is not in Q , push grid into Q
                if(!list.contains(grid)) list.add(grid);
            }
        }
    }

    /* step3:
     * Initializing the high-level Map
     * by create an edge between two exits which are in one room
     */

    public void init_high_level_map(){
        //using an edge list to store the edge between exits
        //ArrayList<Edge> edges = new ArrayList<>();
        for(int r=1 ; r <= rooms_count ; r++){
            //declare room information
            Room room = rooms[r];
            int rows=room.getRows(), cols=room.getCols();
            Grid[][] localMap = room.getGridMap();
            HashMap<Grid,Grid[][]> navigator = room.getNavigators();
            //use an arraylist to collect the exits of a room
            ArrayList<Grid> exits_of_room = new ArrayList<>();
            for(int i = 0; i < rows; i++)
                for(int j = 0; j < cols; j++)
                    if(localMap[i][j].getType()==Grid.EXIT)
                        exits_of_room.add(localMap[i][j]);
            //check if two exits is passable
            for (int i = 0; i < exits_of_room.size(); i++) {
                for (int j = 0; j < exits_of_room.size(); j++) {
                    //grid 1 is 'exit' (end) , grid 2 is head (other exit)
                    Grid grid1 = exits_of_room.get(i), grid2 = exits_of_room.get(j);
                    if(grid1.getX()==grid2.getX()&&grid1.getY()==grid2.getY()) continue;
                    //find the key grid1 from the navigator
                    for (Entry<Grid, Grid[][]> entry : navigator.entrySet()) {
                        Grid key = entry.getKey();          // 取得鍵 (Grid)
                        Grid[][] value = entry.getValue();  // 取得值 (Grid[][])
                        if(!(key.getX()==grid1.getX()&&key.getY()==grid1.getY())) continue;
                        //build an edge between grid1 and grid2
                        //note that the grid which store the right direction
                        //is in the navigator.value
                        int x1=grid1.getX()-room.getPivot_X(), y1=grid1.getY()-room.getPivot_Y();
                        int x2=grid2.getX()-room.getPivot_X(), y2=grid2.getY()-room.getPivot_Y();
                        Edge e = new Edge();
                        //head is the first gird of the trajectory
                        // head-------->tail
                        e.setHead(new Grid(value[x2][y2]));
                        build_edge(e,value[x1][y1],value[x2][y2],value,room);
                        if(e.getTail().getX()==key.getX()&&e.getTail().getY()==key.getY()) edges.add(new Edge(e));
                    }
                }
            }

        }
    }

    public void build_edge(Edge edge, Grid grid1, Grid grid2, Grid[][] map, Room room){
        //set tail with grid2 (if grid1==grid2 then tail==grid1)
        //we can use this condition to know if there is an edge between grid1 and grid2
        edge.setTail(new Grid(grid2));
        //renew the available time of an edge
        double newAvailableTime = grid2.getDeadLine()- edge.getTravelTime();
        edge.setAvailableTime(Math.min(edge.getAvailableTime(),newAvailableTime));
        //two points are the same
        int x1=grid1.getX()-room.getPivot_X(), y1=grid1.getY()-room.getPivot_Y();
        int x2=grid2.getX()-room.getPivot_X(), y2=grid2.getY()-room.getPivot_Y();
        if(x1==x2 && y1==y2) {
            edge.AddTrajectory(grid2);
            return;
        }
        //test code
        //System.out.println("grid2:"+grid2.getX()+","+grid2.getY());
        //System.out.println("deadline:"+grid2.getDeadLine());
        //two points are different
        //we need to next step from grid2's direction
        int direction=grid2.getDirection();
        if(direction==Grid.UP) {
            edge.AddTrajectory(new Grid(grid2));
            edge.addTravelTime(1/runSpeed);
            build_edge(edge,grid1,map[x2-1][y2],map,room);
        } else if(direction==Grid.DOWN) {
            edge.AddTrajectory(new Grid(grid2));
            edge.addTravelTime(1/runSpeed);
            build_edge(edge,grid1,map[x2+1][y2],map,room);
        } else if(direction==Grid.LEFT) {
            edge.AddTrajectory(new Grid(grid2));
            edge.addTravelTime(1/runSpeed);
            build_edge(edge, grid1, map[x2][y2 - 1], map, room);
        } else if(direction==Grid.RIGHT) {
            edge.AddTrajectory(new Grid(grid2));
            edge.addTravelTime(1/runSpeed);
            build_edge(edge, grid1, map[x2][y2+1], map, room);
        } else if(direction==Grid.UP_LEFT) {
            edge.AddTrajectory(new Grid(grid2));
            edge.addTravelTime(1/runSpeed*Math.sqrt(2));
            build_edge(edge, grid1, map[x2-1][y2-1], map, room);
        } else if(direction==Grid.UP_RIGHT) {
            edge.AddTrajectory(new Grid(grid2));
            edge.addTravelTime(1/runSpeed*Math.sqrt(2));
            build_edge(edge, grid1, map[x2-1][y2+1], map, room);
        } else if(direction==Grid.DOWN_LEFT) {
            edge.AddTrajectory(new Grid(grid2));
            edge.addTravelTime(1/runSpeed*Math.sqrt(2));
            build_edge(edge, grid1, map[x2+1][y2-1], map, room);
        } else if(direction==Grid.DOWN_RIGHT) {
            edge.AddTrajectory(new Grid(grid2));
            edge.addTravelTime(1/runSpeed*Math.sqrt(2));
            build_edge(edge, grid1, map[x2+1][y2+1], map, room);
        } else {
            return ;
        }

    }

    /* step4:
     * Construct concise High-Level-Map
     */

    public void concise_high_level_map(){
        Queue<Grid> global_exits = new LinkedList<>();
        //除了最後逃生出口以外的點的deadline都先設為0
        //最後會被更新成基於出口的相對deadline
        for(int i=0;i<ROWS;i++)
            for(int j=0;j<COLS;j++)
                GridMap[i][j].setDeadLine(0);
        //設置出口的deadline
        GridMap[23][10].setDeadLine(GridMap[23][10].getFireTime());
        GridMap[0][10].setDeadLine(GridMap[0][10].getFireTime());
        global_exits.add(GridMap[23][10]);
        global_exits.add(GridMap[0][10]);
        while(!global_exits.isEmpty()){
            Grid ni = global_exits.poll();
            //find an edge which the tail is ni
            for(int i=0;i<edges.size();i++){
                Grid tail=edges.get(i).getTail();
                //choose the edge whose tail is ni
                if(tail.getX()==ni.getX() && tail.getY()==ni.getY()){
                    Grid nk=edges.get(i).getHead();
                    double newDeadLine=Math.min(edges.get(i).getAvailableTime(),ni.getDeadLine()-edges.get(i).getTravelTime());
                    System.out.println(i);
                    System.out.println(edges.get(i).getAvailableTime()+" "+ni.getDeadLine()+" "+edges.get(i).getTravelTime());
                    System.out.println(nk.getDeadLine());
                    //特別注意此處的nk是step2更改過的到局部出口的相對deadline
                    //所以需要改成剛才歸0的部分
                    if(newDeadLine>GridMap[nk.getX()][nk.getY()].getDeadLine()){
                        System.out.println("Hello");
                        nk.setDeadLine(newDeadLine);
                        GridMap[nk.getX()][nk.getY()].setDeadLine(newDeadLine);
                        nk.setOutgoingEdge(edges.get(i));
                        GridMap[nk.getX()][nk.getY()].setOutgoingEdge(edges.get(i));
                        if(!global_exits.contains(nk)){
                            global_exits.add(nk);
                        }
                    }
                }
            }
        }
    }

    /* step5:
     * find the route for user
     * 1.confirm the ID of room in which the user being
     * 2.if there are more than one local exit, choose the one with higher deadline
     * 3.print each grid along the edge
     */
    public void user_guide(int user_x,int user_y){
        for(int i=0;i<ROWS;i++){
            for(int j=0;j<COLS;j++){
                System.out.print(GridMap[i][j].getDeadLine()+" ");
            }
            System.out.println();
        }
        //find the room in which the user being
        int now_room=0;
        for(int r=1;r<=rooms_count;r++){
            Room room=rooms[r];
            int x1=rooms[r].getPivot_X(),x2=rooms[r].getPivot_X()+rooms[r].getRows();
            int y1=rooms[r].getPivot_Y(),y2=rooms[r].getPivot_Y()+rooms[r].getCols();
            if(x1<=user_x&&x2>=user_x&&y1<=user_y&&y2>=user_y){
                now_room=r;
            }
        }
        //get the navigation map of the now_room
        //we need to count which strategy has higher deadline
        Room room = rooms[now_room];
        int rows=room.getRows(), cols=room.getCols();
        Grid[][] localMap = room.getGridMap();
        HashMap<Grid,Grid[][]> navigator = room.getNavigators();
        //use an arraylist to collect the exits of a room
        ArrayList<Grid> exits_of_room = new ArrayList<>();
        for(int i = 0; i < rows; i++)
            for(int j = 0; j < cols; j++)
                if(localMap[i][j].getType()==Grid.EXIT)
                    exits_of_room.add(localMap[i][j]);
        //find the navigation map for one exit
        for(int i=0;i<exits_of_room.size();i++){
            Grid local_exit=exits_of_room.get(i);
            double max_deadline=0;
            Grid[][] guide;
            //find the exit with higher deadline
            for (Entry<Grid, Grid[][]> entry : navigator.entrySet()) {
                Grid key = entry.getKey();          // 取得鍵 (Grid)
                Grid[][] value = entry.getValue();  // 取得值 (Grid[][])
                if (!(key.getX() == local_exit.getX() && key.getY() == local_exit.getY())) continue;
                //now the value is the navigation map for a local exit
                int x1=user_x-rooms[i].getPivot_X(),y1=user_y-rooms[i].getPivot_Y();
                //算出房內任意點到房內出口所耗費的時間
                double temp=count_deadline(key,value[x1][y1],value);
                Edge edge=GridMap[key.getX()][key.getY()].getOutgoingEdge();
                //直到最終出口為止所需花費的總時間
                while(edge!=null){
                    temp+=edge.getTravelTime();
                    key=edge.getTail();
                    edge=key.getOutgoingEdge();
                }
                double deadline=key.getDeadLine()-temp;
                if(deadline>max_deadline){
                    max_deadline=deadline;
                    guide=value;
                }
            }
            //now we have guide and max_deadline
            System.out.println("user:"+"("+user_x+","+user_y+")");
            print_path();
        }
    }

    public double count_deadline(Grid end,Grid start,Grid[][] map){
        double deadline=0;
        int x2=start.getX(),y2=start.getY();
        if(end.getX()==start.getX() && end.getY()==start.getY()){
            return deadline;
        }else{
            int direction=start.getDirection();
            if(direction==Grid.UP) {
                return count_deadline(end,map[x2-1][y2],map)+1/runSpeed;
            } else if(direction==Grid.DOWN) {
                return count_deadline(end,map[x2+1][y2],map)+1/runSpeed;
            } else if(direction==Grid.LEFT) {
                return count_deadline(end,map[x2][y2-1],map)+1/runSpeed;
            } else if(direction==Grid.RIGHT) {
                return count_deadline(end,map[x2][y2+1],map)+1/runSpeed;
            } else if(direction==Grid.UP_LEFT) {
                return count_deadline(end,map[x2-1][y2-1],map)+1/runSpeed*Math.sqrt(2);
            } else if(direction==Grid.UP_RIGHT) {
                return count_deadline(end,map[x2-1][y2+1],map)+1/runSpeed*Math.sqrt(2);
            } else if(direction==Grid.DOWN_LEFT) {
                return count_deadline(end,map[x2+1][y2-1],map)+1/runSpeed*Math.sqrt(2);
            } else if(direction==Grid.DOWN_RIGHT) {
                return count_deadline(end,map[x2+1][y2+1],map)+1/runSpeed*Math.sqrt(2);
            } else {
                return deadline;
            }
        }
    }

    public void print_path(){

    }

    /* Test code:
     * print the result of each exit for every rooms
     */

    public Grid[][] returnMap(){
        HashMap<Grid,Grid[][]> navigator=rooms[5].getNavigators();
        for (Entry<Grid, Grid[][]> entry : navigator.entrySet()) {
            Grid key = entry.getKey();          // 取得鍵 (Grid)
            Grid[][] value = entry.getValue();  // 取得值 (Grid[][])
            for(int i=0;i<rooms[5].getRows();i++){
                for(int j=0;j<rooms[5].getCols();j++){
                    GridMap[i+rooms[5].getPivot_X()][j+rooms[5].getPivot_Y()]=value[i][j];
                }
            }
        }
        navigator=rooms[4].getNavigators();
        for (Entry<Grid, Grid[][]> entry : navigator.entrySet()) {
            Grid key = entry.getKey();          // 取得鍵 (Grid)
            Grid[][] value = entry.getValue();  // 取得值 (Grid[][])
            if(key.getX()==0&&key.getY()==30) {
                for (int i = 0; i < rooms[4].getRows(); i++) {
                    for (int j = 0; j < rooms[4].getCols(); j++) {
                        GridMap[i + rooms[4].getPivot_X()][j + rooms[4].getPivot_Y()] = value[i][j];
                    }
                }

            }
        }
        pickPath(GridMap,user_loc_x,user_loc_y);
        return GridMap;
    }

    public Grid[][] getGridMap(){
        pickPath(GridMap,user_loc_x,user_loc_y);
        return GridMap;
    }

    private void pickPath(Grid[][] gridMap,int x,int y){
        gridMap[x][y].setSelected(true);
        if (x - 1 >= 0 && y - 1 >= 0 && gridMap[x][y].getDirection() == Grid.UP_LEFT) pickPath(gridMap, x - 1, y - 1);
        if (x - 1 >= 0 && gridMap[x][y].getDirection() == Grid.UP) pickPath(gridMap, x - 1, y);
        if (x - 1 >= 0 && y + 1 < gridMap[0].length && gridMap[x][y].getDirection() == Grid.UP_RIGHT) pickPath(gridMap, x - 1, y + 1);
        if (y - 1 >= 0 && gridMap[x][y].getDirection() == Grid.LEFT) pickPath(gridMap, x, y - 1);
        if (y + 1 < gridMap[0].length && gridMap[x][y].getDirection() == Grid.RIGHT) pickPath(gridMap, x, y + 1);
        if (x + 1 < gridMap.length && y - 1 >= 0 && gridMap[x][y].getDirection() == Grid.DOWN_LEFT) pickPath(gridMap, x + 1, y - 1);
        if (x + 1 < gridMap.length && gridMap[x][y].getDirection() == Grid.DOWN) pickPath(gridMap, x + 1, y);
        if (x + 1 < gridMap.length && y + 1 < gridMap[0].length && gridMap[x][y].getDirection() == Grid.DOWN_RIGHT) pickPath(gridMap, x + 1, y + 1);
    }


    public void testNavigator(){
        for(int i=1;i<rooms_count;i++){
            Room room = rooms[i];
            HashMap<Grid,Grid[][]> navigator = room.getNavigators();
            //reversal the navigator
            for (Entry<Grid, Grid[][]> entry : navigator.entrySet()) {
                Grid key = entry.getKey();          // 取得鍵 (Grid)
                Grid[][] value = entry.getValue();  // 取得值 (Grid[][])
                System.out.println("("+key.getX()+","+key.getY()+")");
                for(int m=0;m<value.length;m++){
                    for(int n=0;n<value[m].length;n++){
                        System.out.print(convert(value[m][n].getDirection())+" ");
                    }
                    System.out.println();
                }
                System.out.println();
            }

        }
    }

    public char convert(int direction){
        if (direction == Grid.UP) {
            return '↑';
        } else if (direction == Grid.DOWN) {
            return '↓';
        } else if (direction == Grid.LEFT) {
            return '←';
        } else if (direction == Grid.RIGHT) {
            return '→';
        } else if (direction == Grid.UP_LEFT) {
            return '↖';
        } else if (direction == Grid.UP_RIGHT) {
            return '↗';
        } else if (direction == Grid.DOWN_LEFT) {
            return '↙';
        } else if (direction == Grid.DOWN_RIGHT) {
            return '↘';
        } else {
            return 'x';
        }
    }

    public void testEdge(){
        for(int i=0;i<edges.size();i++){
            Edge edge = edges.get(i);
            ArrayList<Grid> trajectory = edge.getTrajectory();
            System.out.println("num: "+i);
            System.out.print("Start:("+edge.getHead().getX()+","+edge.getHead().getY()+")");
            System.out.println(" End:("+edge.getTail().getX()+","+edge.getTail().getY()+")");
            for(int j=0;j<trajectory.size();j++){
                Grid grid = trajectory.get(j);
                System.out.println("("+grid.getX()+","+grid.getY()+") Deadline: "+grid.getDeadLine());
            }
            //System.out.println();
            System.out.println("Available Time:"+edge.getAvailableTime());
            System.out.println("Travel Time:"+edge.getTravelTime());
            System.out.println();
        }
    }

    public void testOutgoingEdge(){
        for(int i=0;i<ROWS;i++){
            for(int j=0;j<COLS;j++) {
                if(GridMap[i][j].getOutgoingEdge()!=null){
                    System.out.print("now: ("+GridMap[i][j].getX()+","+GridMap[i][j].getY()+")");
                    System.out.print(" edge: ("+GridMap[i][j].getOutgoingEdge().getHead().getX()+","+GridMap[i][j].getOutgoingEdge().getHead().getY()+")");
                    System.out.println(" ("+GridMap[i][j].getOutgoingEdge().getTail().getX()+","+GridMap[i][j].getOutgoingEdge().getTail().getY()+")");
                }
            }
        }
    }
}
