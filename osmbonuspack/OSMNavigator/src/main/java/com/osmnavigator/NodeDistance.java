package com.osmnavigator;
import java.util.*;

/**
 * Created by leesoe on 3/5/17.
 */

public class NodeDistance {
    private List<Boolean> isAscendingDistanceList;
    private int sizeLimit;

    public NodeDistance(int sizeLimit) {
        this.isAscendingDistanceList = new ArrayList<>();
        this.sizeLimit = sizeLimit;
    }

    public void add(boolean bool) {
        if (this.isAscendingDistanceList.size() == 3)
            this.isAscendingDistanceList.remove(0);

        this.isAscendingDistanceList.add(bool);
    }

    public boolean checkIfAscendingDistance() {
        if (isAscendingDistanceList.size() != sizeLimit)
            return false;

        for (boolean isAscending : this.isAscendingDistanceList) {
            if (!isAscending)
                return false;
        }
        return true;
    }

    public void clearList() {
        this.isAscendingDistanceList.clear();
    }


}
