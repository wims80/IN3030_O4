import java.util.Arrays;

class RadixSort {

  // The number of bits used to represent a single digit
  int useBits;
  int[] a, b, digitFrequencies, digitPointers;

  // Used in findGlobalMax
  static int globalMax;

  RadixSort(int useBits) {
    this.useBits = useBits;
  }


  static class FindMaxMulti implements Runnable {
    int[] a;
    int id;
    int numThreads;

    FindMaxMulti(int[] a, int id, int numThreads) {
      this.a = a;
      this.id = id;
      this.numThreads = numThreads;
    }

    public void run() {
      int localMax  = 0;
      for (int i = id; i < a.length; i = i + numThreads)
        if (a[i] > localMax)
          localMax = a[i];

      updateGlobalMax(localMax);
    }

  }

  synchronized static void updateGlobalMax(int localMax) {
    if (localMax > globalMax)
      globalMax = localMax;
  }


  // Counting sort. A stable sorting algorithm.
  private void countingSort(int mask, int shift) {

    // STEP B : Count the number of occurrences of each digit in a specific position.
    digitFrequencies = new int[mask + 1];
    for (int num : a)
      digitFrequencies[(num >> shift) & mask]++;


    // STEP C : Find the start position of each digit in array B.
    digitPointers = new int[mask + 1];
    for (int i = 0; i < digitFrequencies.length - 1; i++)
      digitPointers[i + 1] = digitPointers[i] + digitFrequencies[i];


    // STEP D : Place the numbers in array A, in the correct places of array B
    for (int num : a)
      b[digitPointers[(num >> shift) & mask]++] = num;

  }

  // Radix sort. Uses counting sort for each position.
  int[] radixSort(int[] unsortedArray) {

    a = unsortedArray;
    b = new int[a.length];

    // STEP A : Find the maximum value.
    int max = a[0];

    for (int num : a)
      if (num > max)
        max = num;


    // Substep: Finding number of bits that is needed to represent max value
    int numBitsMax = 1;
    while (max >= (1L << numBitsMax))
      numBitsMax++;


    // Substep: Finding the number of positions needed to represent the max value
    int numOfPositions = numBitsMax / useBits;
    if (numBitsMax % useBits != 0) numOfPositions++;


    // Substep: If useBits is larger than numBitsMax,
    // set useBits equal to numBitsMax to save space.
    if (numBitsMax < useBits) useBits = numBitsMax;


    // Substep: Creating the mask and initialising the shift variable,
    // both of whom are used to extract the digits.
    int mask = (1 << useBits) - 1;
    int shift = 0;


    // Performing the counting sort on each position
    for (int i = 0; i < numOfPositions; i++) {

      countingSort(mask, shift);
      shift += useBits;

      // Setting array a to be the array to be sorted again
      int[] temp = a;
      a = b;
      b = temp;

    }

    return a;

  }

  int[] multiRadixSort(int[] unsortedArray) {
    int numThreads = Runtime.getRuntime().availableProcessors();

    Thread[] threads = new Thread[numThreads];
    for (int i = 0; i < numThreads; i++) {
      threads[i] = new Thread(new FindMaxMulti(unsortedArray, i, numThreads));
      threads[i].start();
    }
    try {
      for (Thread t : threads)
        t.join();
    } catch (Exception e) {
      System.out.println("Caught exception in multiRadixSort :" + e.toString());
    }

    System.out.println("Parallel findMax = " + globalMax);

    return null;
  }


  public static void main(String[] args) {

    int n, seed, useBits;

    try {

      n = Integer.parseInt(args[0]);
      seed = Integer.parseInt(args[1]);
      useBits = Integer.parseInt(args[2]);

    } catch (Exception e) {

      System.out.println("Correct usage is: java RadixSort <n> <seed> <useBits>");
      return;

    }

    // Radix sorting
    int[] a = Oblig4Precode.generateArray(n, seed);
    RadixSort rs = new RadixSort(useBits);
    a = rs.radixSort(a);

    // Quick check to see if sorted (takes a few seconds at high n's)
    int[] arraysort = Oblig4Precode.generateArray(n, seed);
    Arrays.sort(arraysort);
    System.out.println("Arrays are equal: " + Arrays.equals(arraysort, a));


    // MULTICORE

    int[] aMulti = Oblig4Precode.generateArray(n, seed);

    rs.multiRadixSort(aMulti);



  }

}