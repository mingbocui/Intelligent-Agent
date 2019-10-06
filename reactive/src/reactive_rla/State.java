package reactive_rla;

import logist.topology.Topology.City;

import java.util.Objects;

public class State {
    private City fromCity; // define the city where the task is sent from;
    private City toCity; // define the city where the task is sent to;
    private boolean hasTask; // is there any task in the current state;

    public State(City fromCity, City toCity, boolean hasTask){
        this.fromCity = fromCity; // TODO current city??????
        this.toCity = toCity;
        this.hasTask = hasTask;
    }

    public City getToCity() {
        return toCity;
    }

    public City getFromCity() {
        return fromCity;
    }

    public void setFromCity(City fromCity) {
        this.fromCity = fromCity;
    }

    public void setToCity(City toCity) {
        this.toCity = toCity;
    }

    public void setHasTask(boolean hasTask) {
        this.hasTask = hasTask;
    }

    public boolean getHasTask() {
        return hasTask;
    }

    // TODO auto-generated function, logica correctness needed to be checked
    @Override
    public boolean equals(Object o) {
        // TODO sam fix this
        if (this == o) return true;
        if (!(o instanceof State)) return false;
        State state = (State) o;
        return hasTask == state.hasTask &&
                Objects.equals(fromCity, state.fromCity) &&
                Objects.equals(toCity, state.toCity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromCity, toCity, hasTask);
    }


//    @Override
//    public boolean equals(Object obj){
//        State state = (State) obj;
//        if(this.fromCity.equals(state.fromCity) && this.toCity.equals(state.toCity) && this.hasTask != state.hasTask)
//            return true;
//        else
//            return false;
//    }
//    @Override
//    public int hashCode(){
//
//        int res = 17; // just a prime
//
//        //TODO not sure the hashcode of the null object, 0?
//        res = res * 31 + fromCity.hashCode();
//        res = res * 31 + toCity.hashCode();
//        res = res * 31 + (hasTask? 0 : 1);
//
//        return res;
//
//    }


}
