package dominion.core;

import cu.edu.cujae.ceis.graph.LinkedGraph;
import cu.edu.cujae.ceis.graph.interfaces.ILinkedNotDirectedGraph;
import cu.edu.cujae.ceis.graph.vertex.Vertex;
import dominion.model.players.Player;
import dominion.model.territories.Territory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameMap {

    private ILinkedNotDirectedGraph territoriesGraph;
    private ArrayList<Territory> territories;

    public GameMap(){
        territoriesGraph = new LinkedGraph();
        territories = new ArrayList<>();
    }
    //GETTERS AND SETTERS

    //METHOD
    public int findPos(Territory t){
        int pos = -1;
        Iterator<Vertex> it = territoriesGraph.getVerticesList().iterator();
        int i = 0;

        while(it.hasNext() && pos == -1){
            Territory actualTerritory = (Territory) it.next().getInfo();
            if(actualTerritory.equals(t)){
                pos = i;
            }
            else{
                i++;
            }
        }
        return pos;

    }

    public void addTerritory(Territory t){
        territoriesGraph.insertVertex(t);
        territories.addLast(t);
    }

    public void addRoute(Territory t, Territory to){
        int pos1 = findPos(t);
        int pos2 = findPos(to);

        if(pos1 != -1 && pos2 != -1){
            territoriesGraph.insertEdgeNDG(pos1, pos2);
        }

    }

    public boolean areAdjacent(Territory t1, Territory t2) {
        int pos1 = findPos(t1);
        int pos2 = findPos(t2);
        boolean areAdjacent = false;

        if(pos1 != -1 && pos2 != -1){
            areAdjacent = territoriesGraph.areAdjacents(pos1, pos2);
        }
        return areAdjacent;
    }

    public List<Territory> getNeighbours(Territory t) {
        int pos = findPos(t);
        List<Territory> adj = new ArrayList<>();
        if(pos != -1) {
           Vertex v = territoriesGraph.getVerticesList().get(pos);
           for(Vertex vertex:v.getAdjacents()){
               adj.add((Territory)vertex.getInfo());
           }
        }
        return adj;
    }

    public boolean playerCanAttack(Player player, Territory territory){
       int pos = findPos(territory);
       boolean playerCanAttack = false;

       for(int i = 0; i< player.getTerritories().size() && !playerCanAttack;i++){
           int pos1 = findPos(player.getTerritories().get(i));

           if(territoriesGraph.areAdjacents(pos, pos1)){
               playerCanAttack = true;
           }
       }
       return playerCanAttack;

    }
}
