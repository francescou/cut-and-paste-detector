package it.uliana.cpd;

public class Pair { 
	
    public final String x;
    public final String y;
    public final int score;
    
    public Pair(String x, String y, int score) { 
        this.x = x; 
        this.y = y; 
        this.score = score;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + "): " + score;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof Pair)){
            return false;
        }
		Pair other_ = (Pair) other;
        return other_.x == this.x && other_.y == this.y;
    }

    @Override
    public int hashCode() {
        final int p = 23;
        int value = 1;
        value = p * value + ((x == null) ? 0 : x.hashCode());
        value = p * value + ((y == null) ? 0 : y.hashCode());
        return value;
    }
}