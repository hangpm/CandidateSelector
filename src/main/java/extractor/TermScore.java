package extractor;

public class TermScore implements Comparable<TermScore> {
	public String term;
	public float score;

	public TermScore(String term, float score) {
		this.term = term;
		this.score = score;
	}

	public int compareTo(TermScore that) {
		return score < that.score? 1: score == that.score? 0 : -1; // desc
	}

	public String toString() { return term + ":" + score; }
}

