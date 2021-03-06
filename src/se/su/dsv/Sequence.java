package se.su.dsv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

public class Sequence {

	public static List<Sequence> loadFromFile(File inputFile)
			throws IOException {
		List<Sequence> sequences = new LinkedList<Sequence>();
		BufferedReader reader = null;
		try {
			FileInputStream fin = new FileInputStream(inputFile);
			reader = new BufferedReader(new InputStreamReader(fin));
			String line = null;
			while ((line = reader.readLine()) != null) {
				sequences.add(new Sequence(line));
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					throw e;
				}
		}

		return sequences;
	}

	public static List<ResultSequence> loadFromResultFile(String outputFile) {
		List<ResultSequence> temp = new LinkedList<ResultSequence>();
		BufferedReader reader = null;
		try {
			FileInputStream fin = new FileInputStream(new File(outputFile));
			reader = new BufferedReader(new InputStreamReader(fin));
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] split = line.split("SUP:");
				temp.add(new ResultSequence(split[0].trim(), Integer
						.parseInt(split[1].trim())));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}

		return temp;
	}

	public static void writeToFile(List<Sequence> sequences, String outputFile)
			throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
		for (Sequence sequence : sequences) {
			out.write(sequence.toString() + "\n");
		}
		out.close();
	}

	public static Result search(List<Sequence> haystack, Sequence needle) {
		double frequency = 0.0, total = 0.0;
		for (Sequence s : haystack) {
			if (s.contains(needle)) {
				frequency += 1;
			}
			total += 1;
		}

		return new Result(frequency, total);
	}

	private static Random RANDOM = new Random();
	private ArrayList<ItemSet> itemsets = new ArrayList<ItemSet>();

	/**
	 * Build a sequences (non-lazy) and not fault tolerant. That is, the
	 * constructor assumes a correct sequence.
	 * 
	 * @param line
	 */
	public Sequence(String line) {
		StringBuilder itemset = new StringBuilder();
		for (String token : line.split(" ")) {
			if (token.equals("-2")) {
				// EOF sequence
				break;
			} else if (token.equals("-1")) {
				// EOF itemset
				this.itemsets.add(new ItemSet(itemset.toString()));
				itemset = new StringBuilder();
			} else {
				itemset.append(token);
				itemset.append(' ');
			}
		}
	}

	private Sequence(ArrayList<ItemSet> copy) {
		this.itemsets = copy;
	}

	/**
	 * Replaces subsequence a with b if exists. Otherwise, does nothing.
	 * 
	 * @param a
	 * @param b
	 * @return a new {@link Sequence} or this
	 */
	public Sequence replaceConsecutive(Sequence a, Sequence b) {
		int index = indexOfConsecutive(a);
		if (index == -1) {
			return this;
		}

		ArrayList<ItemSet> replaced = new ArrayList<ItemSet>();
		for (int n = 0; n < itemsets.size(); n++) {
			if (n == index) {
				replaced.addAll(n, b.itemsets);
				n += b.itemsets.size() - 1;
			} else {
				replaced.add(itemsets.get(n));
			}
		}

		return new Sequence(replaced);
	}

	/**
	 * Shuffle this sequence
	 * 
	 * @return
	 */
	public Sequence randomize() {
		@SuppressWarnings("unchecked")
		ArrayList<ItemSet> copy = (ArrayList<ItemSet>) itemsets.clone();
		Collections.shuffle(copy);
		return new Sequence(copy);
	}

	public Sequence randomize(Sequence keepStatic) {
		TreeSet<Integer> indexes = indexes(keepStatic);
		if (indexes == null) { // if keepStatic ain't a subsequence
			return randomize(); // just permute..
		}
		ArrayList<ItemSet> random = new ArrayList<ItemSet>();
		ArrayList<Boolean> order = new ArrayList<Boolean>();
		for (int n = 0; n < itemsets.size(); n++) {
			if (!indexes.contains(n)) {
				random.add(itemsets.get(n));
			}
			if (n < indexes.size()) {
				order.add(true);
			} else {
				order.add(false);
			}
		}
		Collections.shuffle(random);
		Collections.shuffle(order);

		// TODO: this is fairly ugly. rewrite
		Iterator<Integer> index = indexes.iterator();
		for (int n = 0; n < order.size(); n++) {
			if (order.get(n)) {
				if (index.hasNext()) {
					Integer i = index.next();
					if (n > random.size() - 1) { // put it last
						random.add(itemsets.get(i));
					} else {
						random.add(n, itemsets.get(i));
					}
				} else {
					break;
				}
			}
		}
		// for (Integer i : indexes) {
		// random.add(i, itemsets.get(i));
		// }
		return new Sequence(random);
	}

	public Sequence replace(Sequence a, Sequence b) {
		Set<Integer> indexes = indexes(a);
		if (indexes == null || a.length() != b.length()) {
			return this;
		}
		int otherIndex = 0;
		ArrayList<ItemSet> replaced = new ArrayList<ItemSet>();
		for (int n = 0; n < itemsets.size(); n++) {
			if (indexes.contains(n)) {
				replaced.add(b.itemsets.get(otherIndex));
				otherIndex += 1;
			} else {
				replaced.add(itemsets.get(n));
			}
		}
		return new Sequence(replaced);
	}

	/**
	 * Permute the sequence
	 * 
	 * @param maxPerm
	 *            - maximum number of permutations to return
	 * @return
	 */
	public List<Sequence> permute(int maxPerm) {
		int n = length();
		/*
		 * Using Stirling's approximation [0] to approximate the number of
		 * permutations as: n! ~ sqrt(2pi*n)(n/e)^n
		 * 
		 * [0] - http://en.wikipedia.org/wiki/Stirling's_approximation
		 */
		long approximateFactorial = Math.round(Math.sqrt(2 * Math.PI * n)
				* Math.pow(n / Math.E, n));
		if (approximateFactorial < maxPerm) {
			return permutations();
		} else {
			ArrayList<Sequence> sequences = new ArrayList<Sequence>();
			for (int n1 = 0; n1 < maxPerm; n1++) {
				sequences.add(randomize());
			}
			return sequences;
		}
	}

	/**
	 * Returns the permutations of the sequence. Although a quite efficent
	 * implementation using the QuickPerm algorithm is used, it's not
	 * recommended for sequences longer than perhaps 8 itemsets (8! = 40320
	 * permutations). Instead use, permute(int) which gives at maximum n
	 * "permutations".
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<Sequence> permutations() {
		ArrayList<String> a = (ArrayList<String>) itemsets.clone();
		int n = a.size();
		int[] p = new int[n]; // Index control array initially all zeros
		int i = 1;
		ArrayList<Sequence> randomSequences = new ArrayList<Sequence>();
		while (i < n) {
			if (p[i] < i) {
				int j = ((i % 2) == 0) ? 0 : p[i];
				Collections.swap(a, i, j);
				randomSequences
						.add(new Sequence((ArrayList<ItemSet>) a.clone()));
				p[i]++;
				i = 1;
			} else {
				p[i] = 0;
				i++;
			}
		}

		return randomSequences;
	}

	public Sequence randomizeConsecutive(Sequence keepStatic) {
		int firstIndex = indexOfConsecutive(keepStatic);
		if (firstIndex == -1)
			return randomize();
		int lastIndex = firstIndex + keepStatic.length();

		ArrayList<ItemSet> randomItemsets = new ArrayList<ItemSet>();
		for (int n = 0; n < itemsets.size(); n++) {
			if (n >= firstIndex && n < lastIndex)
				continue;
			randomItemsets.add(itemsets.get(n));
		}
		Collections.shuffle(randomItemsets);
		randomItemsets.addAll(RANDOM.nextInt(randomItemsets.size()),
				keepStatic.itemsets);

		return new Sequence(randomItemsets);
	}

	public Sequence subSequence(int start, int end) {
		if (start > end || end > itemsets.size() || start < 0) {
			return null;
		}

		ArrayList<ItemSet> subSequence = new ArrayList<ItemSet>();
		for (int n = 0; n < itemsets.size(); n++) {
			if (n >= start && n < end) {
				subSequence.add(itemsets.get(n));
			}
			if (n >= end) {
				break;
			}
		}
		return new Sequence(subSequence);
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Sequence) {
			if (this == obj)
				return true;
			return toString().equals(obj.toString());
		} else {
			return false;
		}
	}

	/**
	 * Return the first indexOf the other sequence. Returns -1 if the sequences
	 * ain't found.
	 * 
	 * @param other
	 * @return
	 */
	public int indexOfConsecutive(Sequence other) {
		int otherIndex = 0;
		int indexOf = -1;
		int otherLength = other.length();
		for (int n = 0; n < itemsets.size(); n++) {
			if (itemsets.get(n).contains(other.itemsets.get(otherIndex))) {
				otherIndex += 1;
				if (indexOf == -1)
					indexOf = n;
			} else {
				otherIndex = 0;
				indexOf = -1;
			}

			if (otherIndex == otherLength)
				return indexOf;
		}
		if (otherIndex == otherLength)
			return indexOf;
		else
			return -1;
	}

	/**
	 * Return the indexes of the individual itemsets of other in this
	 * 
	 * @param other
	 * @return
	 */
	public TreeSet<Integer> indexes(Sequence other) {
		TreeSet<Integer> indexes = new TreeSet<Integer>();
		int otherIndex = 0, otherLength = other.length();
		for (int n = 0; n < itemsets.size(); n++) {
			if (itemsets.get(n).contains(other.itemsets.get(otherIndex))) {
				otherIndex += 1;
				indexes.add(n);
			}
			if (otherIndex == otherLength)
				return indexes;
		}
		return otherIndex == otherLength ? indexes : null;

	}

	public boolean contains(Sequence other) {
		int otherIndex = 0, otherLength = other.length();
		if (otherLength > length()) {
			return false;
		}
		for (int n = 0; n < itemsets.size(); n++) {
			if (itemsets.get(n).contains(other.itemsets.get(otherIndex))) {
				otherIndex += 1;
			}
			if (otherIndex == otherLength)
				break;
		}
		return otherIndex == otherLength;
	}

	public boolean containsConsecutive(Sequence other) {
		return indexOfConsecutive(other) != -1;
	}

	public ItemSet itemset(int index) {
		return itemsets.get(index);
	}

	public int length() {
		return itemsets.size();
	}

	@Override
	public String toString() {
		StringBuilder sequence = new StringBuilder();
		for (ItemSet item : itemsets) {
			sequence.append(item);
			sequence.append("-1 ");
		}
		sequence.append("-2");
		return sequence.toString();
	}

	public String prettyPrint() {
		StringBuilder sequence = new StringBuilder();
		for (ItemSet item : itemsets) {
			sequence.append(item);
			sequence.append(", ");
		}
		sequence.delete(sequence.length() - 2, sequence.length());
		return sequence.toString();
	}
}
