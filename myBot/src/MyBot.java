import com.google.gson.Gson;
import core.*;
import core.api.*;
import core.api.commands.Direction;
import java.util.Comparator;
import java.util.PriorityQueue;


public class MyBot implements Bot {

    InitialData data;

    // Called only once before the match starts. It holds the
    // data that you may need before the game starts.
    @Override
    public void setup(InitialData data) {
        System.out.println((new Gson()).toJson(data));
        this.data = data;

        // Print out the map
        for (int y = data.mapHeight - 1; y >= 0; y--) {
            for (int x = 0; x < data.mapWidth; x++) {
                System.out.print((data.map[y][x]) ? "_" : "#");
            }
            System.out.println();
        }
    }

    // Called repeatedly while the match is generating. Each
    // time you receive the current match state and can use
    // response object to issue your commands.
    @Override
    public void update(MatchState state, Response response) {
        // Find and send your unit to a random direction that
        // moves it to a valid field on the map
        //current location of my bot:
        int currX=state.yourUnit.x;
        int currY=state.yourUnit.y;

        Coin target; //first coin that first bot wants to get

        int d1=distance(state.yourUnit.x,state.yourUnit.y,state.coins[0].x,state.coins[0].y);
        int d2=distance(state.opponentUnit.x,state.opponentUnit.y,state.coins[0].x,state.coins[0].y);
        if(d1<d2)
            target = state.coins[0];
        else
            target = state.coins[1];


        Direction direction = astar(state, target);
        response.moveUnit(direction);

    }

    public Direction astar(MatchState state, Coin target) {    //it will return the direction of the next move
        // create priority queue
        PriorityQueue<Node> q = new PriorityQueue<Node>(new NodeComparator());

        Node[][] map = new Node[data.mapHeight][data.mapWidth];

        for(int i=0; i<data.mapHeight; i++) //y coordinate [y][x]
            for(int j=0; j<data.mapWidth; j++) //x copordinate
                map[i][j]=new Node(j,i);

        q.add(map[state.yourUnit.y][state.yourUnit.x]);
        map[state.yourUnit.y][state.yourUnit.x].distanceStart=0;
        map[state.yourUnit.y][state.yourUnit.x].approximation=distance(state.yourUnit.x,state.yourUnit.y,target.x,target.y);
        //directions:
        // up, right, down, left
        int[] dx={0,1,0,-1};
        int[] dy={1,0,-1,0};
        while(q.peek() != null) {
            Node a=q.poll(); //current node
            a.visited=true;
            if(a.x==target.x && a.y==target.y)
                break;


            for(int k=0; k<4; k++){
                int nodex=a.x;
                int nodey=a.y;
                Direction d = Direction.LEFT;
                switch(k){
                    case 0:
                        nodex+=dx[k];
                        nodey+=dy[k];
                        d=Direction.UP;
                        break;
                    case 1:
                        nodex+=dx[k];
                        nodey+=dy[k];
                        d=Direction.RIGHT;
                        break;
                    case 2:
                        nodex+=dx[k];
                        nodey+=dy[k];
                        d=Direction.DOWN;
                        break;
                    case 3:
                        nodex+=dx[k];
                        nodey+=dy[k];
                        d=Direction.LEFT;
                        break;
                }

                if(check(a.x,a.y,d) && !map[nodey][nodex].visited){
                    map[nodey][nodex].x=nodex;
                    map[nodey][nodex].y=nodey;
                    map[nodey][nodex].direction=d;
                    map[nodey][nodex].previousx=a.x;
                    map[nodey][nodex].previousy=a.y;
                    map[nodey][nodex].distanceStart=a.distanceStart + 1;
                    map[nodey][nodex].approximation=distance(nodex,nodey,target.x,target.y)+map[nodey][nodex].distanceStart;
                    if(checkSaws(nodex,nodey,a.distanceStart+1,state) == false)
                        map[nodey][nodex].approximation+=10;
                    q.add(map[nodey][nodex]);
                }
            }

        }

        int x = target.x;
        int y = target.y;

        while(state.yourUnit.x!=map[y][x].previousx || state.yourUnit.y!=map[y][x].previousy){
            int oldx=x;
            x=map[y][x].previousx;
            y=map[y][oldx].previousy;
        }

        return map[y][x].direction;
    }

    // return manhattan distance
    public int distance(int x1, int y1, int x2, int y2) {
        return Math.abs(x1-x2)+Math.abs(y1-y2);
    }

    //Functions that check if the coordinates will be out of the grid or if I will go in a hole
    public boolean check(int x, int y, Direction direction){
        int newx=x, newy=y;
        switch(direction){
            case UP:
                newy+=1;
                break;
            case DOWN:
                newy-=1;
                break;
            case LEFT:
                newx-=1;
                break;
            case RIGHT:
                newx+=1;
                break;
        }

        if(newx>=data.mapWidth || newx<0 || newy>=data.mapHeight || newy<0 || !data.map[newy][newx])
            return false;
        return true;
    }

    public boolean checkSaws(int nodex, int nodey, int steps, MatchState state){
        for(int i=0; i<state.saws.length; i++){
            int x=state.saws[i].x;
            int y=state.saws[i].y;
            switch(state.saws[i].direction){
                case UP_LEFT:
                    x+=steps*(-1);
                    y+=steps;
                    break;
                case UP_RIGHT:
                    x+=steps;
                    y+=steps;
                    break;
                case DOWN_LEFT:
                    x+=steps*(-1);
                    y+=steps*(-1);
                    break;
                case DOWN_RIGHT:
                    x+=steps;
                    y+=steps*(-1);
                    break;
                default:
                    break;
            }

            if(nodex==x && nodey==y)
                return false;
        }
        return true;
    }

    // Connects your bot to match generator, don't change it.
    public static void main(String[] args) throws Exception {
        NetworkingClient.connectNew(args, new MyBot());
    }
}

class Node {
    int x;
    int y;
    int distanceStart; // actual distance from start to the node (considering holes)
    int approximation; // it is sum of actual distance from the start to the node and the approximation from node to end (just manhattan distance)
    int previousx; // x coordinate of previous node
    int previousy;
    Direction direction; // next move of the previous node
    boolean visited;

    public Node(int x, int y) {
        this.x=x;
        this.y=y;
        this.previousx=-1;
        this.previousy=-1;
        this.distanceStart=-1;
        this.approximation=-1;
        this.direction=null;
        this.visited = false;
    }
}

class NodeComparator implements Comparator<Node> {
    public int compare(Node a, Node b) {
        return a.approximation - b.approximation;
    }
}
