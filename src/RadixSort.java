import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

class RadixSort {
  static int NUM_REPETITIONS = 21;

  // The number of bits used to represent a single digit
  int useBits;
  int[] a, b, digitFrequencies, digitPointers;

  int[] aMulti, bMulti;

  // Used in findGlobalMax
  static int globalMax;

  CyclicBarrier cyclicBarrier;

  // Used for step b)
  int[][] allCount;


  // Used for array comparison
  int[] sequentialSorted;
  int[] multiSorted;




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

  class RadixSortMulti implements Runnable {
    int[] unsortedArray;
    int id;
    int numThreads;
    int localUseBits;

    RadixSortMulti(int[] unsortedArray, int id, int numThreads, int useBits) {
      this.unsortedArray = unsortedArray;
      this.id = id;
      this.numThreads = numThreads;
      this.localUseBits = useBits;
    }

    public void run() {

      //////////////// STEP A
      int[] ar = radixSortMulti(unsortedArray);
      /////////////// END STEP A
      if (id == 0) multiSorted = ar;
    }

    int[] radixSortMulti(int[] unSortedArray) {
      aMulti = unSortedArray;
      bMulti = new int[aMulti.length];

      // STEP A: Find the maximum value
      int localMax  = 0;

      int start = getStart(aMulti);
      int stop = getStop(aMulti);

      for (int i = start; i < stop; i++)
        if (aMulti[i] > localMax)
          localMax = aMulti[i];

      updateGlobalMax(localMax);

      try {
        cyclicBarrier.await();
      } catch (InterruptedException e) {
        System.out.println("InterruptedException! " + e.toString());
      } catch (BrokenBarrierException e) {
        System.out.println("BrokenBarrierException! " + e.toString());
      }

      // find mask
      int numBitsMax = 1;
      while (globalMax >= (1L << numBitsMax))
        numBitsMax++;
      int numOfPositions = numBitsMax / localUseBits;
      if (numBitsMax % localUseBits != 0) numOfPositions++;
      if (numBitsMax < localUseBits) localUseBits = numBitsMax;
      int mask = (1 << localUseBits) - 1;
      int shift = 0;

      // start countingSortMulti for each mask
      for (int i = 0; i < numOfPositions; i++) {
        countingSortMulti(mask, shift);
        shift += localUseBits;

        try {
          cyclicBarrier.await();
        } catch (InterruptedException e) {
          System.out.println("InterruptedException! " + e.toString());
        } catch (BrokenBarrierException e) {
          System.out.println("BrokenBarrierException! " + e.toString());
        }
        if (id == 0) {
          // Setting array a to be the array to be sorted again
          int[] temp = aMulti;
          aMulti = bMulti;
          bMulti = temp;
        }
        try {
          cyclicBarrier.await();
        } catch (InterruptedException e) {
          System.out.println("InterruptedException! " + e.toString());
        } catch (BrokenBarrierException e) {
          System.out.println("BrokenBarrierException! " + e.toString());
        }
      }
      return aMulti;
    }

    void countingSortMulti(int mask, int shift) {

      /////////////// STEP B
      // counts the amount of each digit

      int start, stop;

      start = getStart(aMulti);
      stop = getStop(aMulti);

      int[] count = new int[mask + 1];

      for (int i = start; i < stop; i++) {
        count[(aMulti[i] >> shift) & mask]++;
      }

      allCount[id] = count;

      try {
        cyclicBarrier.await();
      } catch (InterruptedException e) {
        System.out.println("InterruptedException! " + e.toString());
      } catch (BrokenBarrierException e) {
        System.out.println("BrokenBarrierException! " + e.toString());
      }

      //////////////// END STEP B

      //////////////// STEP C
      // create pointers from counts

      int[] localPointers = new int[mask + 1];
      for (int t = 0; t < localPointers.length; t++) {
        for (int r = 0; r < allCount.length; r++) {
          for (int s = 0; s < t; s++) {
            localPointers[t] += allCount[r][s];
          }
        }
        for (int r = 0; r < id; r++) {
          localPointers[t] += allCount[r][t];
        }
      }

      /////////////// END STEP C

      /////////////// STEP D
      // move elements based on the pointers

      start = getStart(aMulti);
      stop = getStop(aMulti);
      for (int i = start; i < stop; i++)
        bMulti[localPointers[((aMulti[i] >> shift) & mask)]++] = aMulti[i];

    }

    int getStart(int[] array) {
      return ((array.length / numThreads) * id);
    }

    int getStop(int[] array) {
      if (id != numThreads - 1) return((array.length / numThreads) * (id + 1));
      else return (array.length);
    }

  }

  synchronized static void updateGlobalMax(int localMax) {
    if (localMax > globalMax)
      globalMax = localMax;
  }

  void multiRadixSort(int[] unsortedArray, int useBits) {

    int numThreads = Runtime.getRuntime().availableProcessors();

    cyclicBarrier = new CyclicBarrier(numThreads);
    allCount = new int[numThreads][];


    Thread[] threads = new Thread[numThreads];
    for (int i = 0; i < numThreads; i++) {
      threads[i] = new Thread(new RadixSortMulti(unsortedArray, i, numThreads, useBits));
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
  }

  static void compareArrays(int[] a, int[] b) {
    boolean equal = true;
    for (int i = 0; i < a.length; i++) {
      if (a[i] != b[i]) {
        System.out.println("The element at position " + i + " is not the same between a and b");
        System.out.println("a[" + i + "] == " + a[i]);
        System.out.println("b[" + i + "] == " + b[i]);
        equal = false;
      }
    }
    if (equal) System.out.println("The parallel and the sequentially sorted arrays are identical!");
    else System.out.println("The parallel and the sequentially sorted arrays are not identical :(");
  }

  static boolean isSorted(int[] a) {
    for (int i = 1; i < a.length; i++) {
      if (!(a[i - 1] <= a[i])) {
        System.out.println("Array not in order starting at index " + i);
        return false;
      }
    }
    return true;
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

    float[] sequentialTimes = new float[NUM_REPETITIONS];
    float[] parallelTimes = new float[NUM_REPETITIONS];

    // Sequential
    int[] unsortedArray = Oblig4Precode.generateArray(n, seed);

    RadixSort rs = new RadixSort(useBits);

    for (int i = 0; i < NUM_REPETITIONS; i++) {
      long start, stop;
      start = System.nanoTime();
      rs.sequentialSorted = rs.radixSort(unsortedArray);
      stop = System.nanoTime();
      sequentialTimes[i] = (float)(stop - start) / 1000000;
      System.out.println("Sequential time run #" + i + " : " + sequentialTimes[i] + "ms");
    }

    // Parallel
    int[] multiUnsortedArray = Oblig4Precode.generateArray(n, seed);
    for (int i = 0; i < NUM_REPETITIONS; i++) {
      long start, stop;
      start = System.nanoTime();
      rs.multiRadixSort(multiUnsortedArray, useBits);
      stop = System.nanoTime();
      parallelTimes[i] = (float)(stop - start) / 1000000;
      System.out.println("Parallel time run #" + i + "   : " + parallelTimes[i] + "ms");
    }

    // Median output
    Arrays.sort(sequentialTimes);
    Arrays.sort(parallelTimes);
    System.out.println("\nMedian sequential time : " + sequentialTimes[NUM_REPETITIONS / 2] + "ms");
    System.out.println("Median parallel time   : " + parallelTimes[NUM_REPETITIONS / 2] + "ms");
    System.out.println("Speedup                : " + (sequentialTimes[NUM_REPETITIONS / 2] / parallelTimes[NUM_REPETITIONS / 2]) + "x");

    // Checks if the arrays are in order
    if (isSorted(rs.sequentialSorted))
      System.out.println("The sequential array is sorted");
    else
      System.out.println("The sequential array is not sorted");

    if (isSorted(rs.sequentialSorted))
      System.out.println("The parallel array is sorted");
    else
      System.out.println("The parallel array is not sorted");

    // compares two arrays
    compareArrays(rs.sequentialSorted, rs.multiSorted);

    Oblig4Precode.saveResults(Oblig4Precode.Algorithm.SEQ, seed, rs.sequentialSorted);
    Oblig4Precode.saveResults(Oblig4Precode.Algorithm.PAR, seed, rs.multiSorted);



  }
}