import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

class RadixSort {

  // The number of bits used to represent a single digit
  int useBits;
  int[] a, b, digitFrequencies, digitPointers;

  // Used in findGlobalMax
  static int globalMax;

  CyclicBarrier cyclicBarrier;

  // Used for step b)
  int[][] allCount;
  int[] sumCount;

  // Used for step c)
  int[] multiPointers;


  RadixSort(int useBits) {
    this.useBits = useBits;
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
    for (int num : a) {
      int numShiftedAndMasked = (num >> shift) & mask;
      int pos = digitPointers[numShiftedAndMasked]++;
      b[pos] = num;
    }

    //System.out.println("DEBUG");
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


  class FindMaxMulti implements Runnable {
    int[] unsortedArray, a, b;
    int id;
    int numThreads;
    int localUseBits;

    FindMaxMulti(int[] unsortedArray, int id, int numThreads, int useBits) {
      this.unsortedArray = unsortedArray;
      this.id = id;
      this.numThreads = numThreads;
      this.localUseBits = useBits;
    }

    public void run() {

      //////////////// STEP A
      radixSortMulti(unsortedArray);
      /////////////// END STEP A


      //System.out.println("stop thread " + id);

    }

    private void countingSortMulti(int mask, int shift) {
      int start, stop;
      int[] count;

      start = (a.length / numThreads) * id;
      if (id != numThreads - 1) stop = (a.length / numThreads) * (id + 1);
      else stop = a.length;

      count = new int[mask + 1];
      sumCount = new int[mask + 1];

      //System.out.println("(" + id + ") start = " + start + ", stop = " + stop);

      for (int i = start; i < stop; i++) {
        count[(a[i] >> shift) & mask]++;
      }

      allCount[id] = count;

      try {
        cyclicBarrier.await();
      } catch (InterruptedException e) {
      } catch (BrokenBarrierException e) {
      }

      start = (sumCount.length / numThreads) * id;
      if (id != numThreads - 1) stop = (sumCount.length / numThreads) * (id + 1);
      else stop = sumCount.length;

      //System.out.println("(" + id + ") start = " + start + ", stop = " + stop);

      for (int i = 0; i < numThreads; i++) {
        for (int j = start; j < stop; j++) {
          sumCount[j] += allCount[i][j];
        }
      }

      multiPointers = new int[mask + 1];
      try {
        cyclicBarrier.await();
      } catch (InterruptedException e) {
      } catch (BrokenBarrierException e) {
      }

      ////////////// END STEP B

      ////////////// STEP C

      // STEP C : Find the start position of each digit in array B.
      digitPointers = new int[mask + 1];
      for (int i = 0; i < digitFrequencies.length - 1; i++)
        digitPointers[i + 1] = digitPointers[i] + digitFrequencies[i];

      for (int i = start; i < stop - 1; i++) {

      }

    }

    int[] radixSortMulti(int[] unSortedArray) {
      a = unSortedArray;
      b = new int[a.length];

      // STEP A: Find the maximum value
      int localMax  = 0;

      for (int i = id; i < a.length; i = i + numThreads)
        if (a[i] > localMax)
          localMax = a[i];

      updateGlobalMax(localMax);

      try {
        cyclicBarrier.await();
      } catch (InterruptedException e) {
      } catch (BrokenBarrierException e) {
      }

      // Substep: Finding number of bits that is needed to represent max value
      int numBitsMax = 1;
      while (globalMax >= (1L << numBitsMax))
        numBitsMax++;

      // Substep: Finding the number of positions needed to represent the max value
      int numOfPositions = numBitsMax / localUseBits;
      if (numBitsMax % localUseBits != 0) numOfPositions++;

      // Substep: If useBits is larger than numBitsMax,
      // set useBits equal to numBitsMax to save space.
      if (numBitsMax < localUseBits) localUseBits = numBitsMax;

      // Substep: Creating the mask and initialising the shift variable,
      // both of whom are used to extract the digits.
      int mask = (1 << localUseBits) - 1;
      //int mask = (1 << localUseBits) - 1;
      int shift = 0;

      // Performing the counting sort on each position
      //for (int i = 0; i < 1; i++) {
      for (int i = 0; i < numOfPositions; i++) {

        countingSortMulti(mask, shift);
        shift += localUseBits;

        // Setting array a to be the array to be sorted again
        int[] temp = a;
        a = b;
        b = temp;

      }

      return a;
    }

  }

  synchronized static void updateGlobalMax(int localMax) {
    if (localMax > globalMax)
      globalMax = localMax;
  }


  int[] multiRadixSort(int[] unsortedArray, int useBits) {
    int numThreads = Runtime.getRuntime().availableProcessors();


    cyclicBarrier = new CyclicBarrier(numThreads);
    allCount = new int[numThreads][];


    Thread[] threads = new Thread[numThreads];
    for (int i = 0; i < numThreads; i++) {
      threads[i] = new Thread(new FindMaxMulti(unsortedArray, i, numThreads, useBits));
      threads[i].start();
    }

    try {
      for (Thread t : threads)
        t.join();
    } catch (Exception e) {
      System.out.println("Caught exception in multiRadixSort :" + e.toString());
      System.out.println("Stack trace : ");
      e.printStackTrace();
    }

    //System.out.println("Parallel findMax = " + globalMax);

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

    int NUM_REPETITIONS = 5;
    long[] sequentialTimes = new long[NUM_REPETITIONS];
    long[] parallelTimes = new long[NUM_REPETITIONS];

    // Radix sorting
    int[] a = null;
    int[] unsortedArray = Oblig4Precode.generateArray(n, seed);
    int[] customArray = {170, 61, 512, 503, 693, 703, 154, 275, 765, 87, 897, 677, 509, 908};
    unsortedArray = customArray;

    RadixSort rs = new RadixSort(useBits);

    for (int i = 0; i < NUM_REPETITIONS; i++) {
      long start, stop;
      start = System.nanoTime();
      a = rs.radixSort(unsortedArray);
      stop = System.nanoTime();
      sequentialTimes[i] = stop - start;
      System.out.println("Sequential time run #" + i + " : " + sequentialTimes[i]);
    }


    // Quick check to see if sorted (takes a few seconds at high n's)
    int[] arraysort = Oblig4Precode.generateArray(n, seed);
    arraysort = customArray;
    Arrays.sort(arraysort);
    System.out.println("Arrays are equal: " + Arrays.equals(arraysort, a));


    // MULTICORE

    for (int i = 0; i < NUM_REPETITIONS; i++) {
      long start, stop;
      start = System.nanoTime();
      rs.multiRadixSort(unsortedArray, useBits);
      stop = System.nanoTime();
      parallelTimes[i] = stop - start;
      System.out.println("Parallel time run #" + i + "   : " + parallelTimes[i]);
    }

    Arrays.sort(sequentialTimes);
    Arrays.sort(parallelTimes);
    System.out.println("Median sequential time : " + sequentialTimes[NUM_REPETITIONS / 2]);
    System.out.println("Median parallel time   : " + parallelTimes[NUM_REPETITIONS / 2]);
    System.out.println("Speedup                : " + ((double)sequentialTimes[NUM_REPETITIONS / 2] / (double)parallelTimes[NUM_REPETITIONS / 2]));



  }

}