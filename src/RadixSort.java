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

  int[] maxSum;


  // Used for debugging
  int[] sequentialSorted;
  //int[] digitPointersCopy;
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


    //digitPointersCopy = digitPointers.clone();
    // STEP D : Place the numbers in array A, in the correct places of array B
    for (int num : a) {
      int numShiftedAndMasked = (num >> shift) & mask;
      int pos = digitPointers[numShiftedAndMasked]++;
      b[pos] = num;
    }

    System.out.println("DEBUG");
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
    int[] unsortedArray;
    int id;
    int numThreads;
    int localUseBits;
    int[] delSum;

    FindMaxMulti(int[] unsortedArray, int id, int numThreads, int useBits) {
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

    void countingSortMulti(int mask, int shift) {
      int start, stop;
      int[] count;

      ////////////////// STEP B

      start = (a.length / numThreads) * id;
      if (id != numThreads - 1) stop = (a.length / numThreads) * (id + 1);
      else stop = a.length;

      count = new int[mask + 1];

      for (int i = start; i < stop; i++) {
        //count[(a[i] >> shift) & mask]++;
        count[(unsortedArray[i] >> shift) & mask]++;
      }

      allCount[id] = count;

      try {
        cyclicBarrier.await();
      } catch (InterruptedException e) {
        System.out.println("InterruptedException! " + e.toString());
      } catch (BrokenBarrierException e) {
        System.out.println("BrokenBarrierException! " + e.toString());
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
        System.out.println("InterruptedException! " + e.toString());
      } catch (BrokenBarrierException e) {
        System.out.println("BrokenBarrierException! " + e.toString());
      }

      ///////  END STEP B


      /////// STEP C


      int currentValue = 0;
      if (id == numThreads - 1) stop--;
      for (int i = start; i < stop; i++) {
        currentValue += sumCount[i];
        delSum[i + 1] = currentValue;
      }
      maxSum[id] = currentValue;

      try {
        cyclicBarrier.await();
      } catch (InterruptedException e) {
        System.out.println("InterruptedException! " + e.toString());
      } catch (BrokenBarrierException e) {
        System.out.println("BrokenBarrierException! " + e.toString());
      }

      int startValue = 0;
      for (int i = 0; i < id; i++) {
        startValue += maxSum[i];
      }

      if (id == numThreads - 1) stop++;
      for (int i = start; i < stop; i++) {
        currentValue = startValue + delSum[i];
        multiPointers[i] = currentValue;
      }

      try {
        cyclicBarrier.await();
      } catch (InterruptedException e) {
        System.out.println("InterruptedException! " + e.toString());
      } catch (BrokenBarrierException e) {
        System.out.println("BrokenBarrierException! " + e.toString());
      }


      ////// END STEP C

      /////// STEP D

      start = (a.length / numThreads) * id;
      if (id != numThreads - 1) stop = (a.length / numThreads) * (id + 1);
      else stop = a.length;

      //System.out.println("(" + id + ") start = " + start + ", stop = " + stop);

      for (int i = start; i < stop; i++) {
        int num = a[i];
        int numShiftedAndMasked = (num >> shift) & mask;
        int pos = multiPointers[numShiftedAndMasked]++;
        b[pos] = num;
      }

      try {
        cyclicBarrier.await();
      } catch (InterruptedException e) {
        System.out.println("InterruptedException! " + e.toString());
      } catch (BrokenBarrierException e) {
        System.out.println("BrokenBarrierException! " + e.toString());
      }

      /*
      //if (stop == localCount.length) stop--;
      for (int t = start; t < stop; t++) {
        for (int r = 0; r < allCount.length; r++) {
          for (int s = 0; s < t; s++) {
            localCount[t] += allCount[r][s];
          }
        }
        for (int r = 0; r < id; r++) {
          localCount[t] += allCount[r][t];
        }
      }
      */
/*
      if (stop == localCount.length) stop--;
      for (int t = start; t < stop; t++) {
        for (int r = 0; r < allCount.length; r++) {
          for (int s = 0; s < t; s++) {
            localCount[t + 1] += allCount[r][s];
          }
        }
        for (int r = 0; r < id; r++) {
          localCount[t + 1] += allCount[r][t];
        }
      }


      try {
        cyclicBarrier.await();
      } catch (InterruptedException e) {
      } catch (BrokenBarrierException e) {
      }

      addToGlobalCount(localCount);

      try {
        cyclicBarrier.await();
      } catch (InterruptedException e) {
      } catch (BrokenBarrierException e) {
      }


      ///////////// END STEP C

      ///////////// STEP D

      start = (a.length / numThreads) * id;
      if (id != numThreads - 1) stop = (a.length / numThreads) * (id + 1);
      else stop = a.length;


      for (int i = start; i < stop; i++) {
        int num = a[i];
        int numShiftedAndMasked = (num >> shift) & mask;
        int pos = multiPointers[numShiftedAndMasked]++;
        b[pos] = num;
      }


      try {
        cyclicBarrier.await();
      } catch (InterruptedException e) {
      } catch (BrokenBarrierException e) {
      }
*/
          //System.out.println("Finished temp");

    }
/*
    void addToGlobalCount(int[] localCount){
      //int prev = -1;
      for (int i = 1; i < localCount.length; i++) {
//        if (prev > 0 && localCount[i] < prev) return;
        if (localCount[i] > 0) {
          multiPointers[i] = localCount[i];
//          prev = localCount[i];
        }
      }
    }
*/

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
        System.out.println("InterruptedException! " + e.toString());
      } catch (BrokenBarrierException e) {
        System.out.println("BrokenBarrierException! " + e.toString());
      }

      int numBitsMax = 1;
      while (globalMax >= (1L << numBitsMax))
        numBitsMax++;
      int numOfPositions = numBitsMax / localUseBits;
      if (numBitsMax % localUseBits != 0) numOfPositions++;
      if (numBitsMax < localUseBits) localUseBits = numBitsMax;
      int mask = (1 << localUseBits) - 1;
      int shift = 0;


      for (int i = 0; i < numOfPositions; i++) {
        maxSum = new int[numThreads];
        delSum = new int[mask + 1];
        sumCount = new int[mask + 1];

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
          int[] temp = a;
          a = b;
          b = temp;
        }
        try {
          cyclicBarrier.await();
        } catch (InterruptedException e) {
          System.out.println("InterruptedException! " + e.toString());
        } catch (BrokenBarrierException e) {
          System.out.println("BrokenBarrierException! " + e.toString());
        }
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
    b = new int[unsortedArray.length];


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

    return a;
  }

  public static void compareArrays(int[] a, int[] b) {
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

    int NUM_REPETITIONS = 1;
    long[] sequentialTimes = new long[NUM_REPETITIONS];
    long[] parallelTimes = new long[NUM_REPETITIONS];

    // Radix sorting
    int[] a = null;
    int[] unsortedArray = Oblig4Precode.generateArray(n, seed);
    //int[] customArray = {1700, 610, 512, 503, 693, 703, 1540, 2750, 765, 87, 897, 677, 509, 908};
    int[] customArray = {9, 2, 4, 7, 8, 5, 1, 1, 3, 6};
    //unsortedArray = customArray.clone();

    RadixSort rs = new RadixSort(useBits);

    for (int i = 0; i < NUM_REPETITIONS; i++) {
      long start, stop;
      start = System.nanoTime();
      a = rs.radixSort(unsortedArray);
      stop = System.nanoTime();
      sequentialTimes[i] = stop - start;
      System.out.println("Sequential time run #" + i + " : " + sequentialTimes[i]);
    }

    rs.sequentialSorted = a.clone();


    // Quick check to see if sorted (takes a few seconds at high n's)
    int[] arraysort = Oblig4Precode.generateArray(n, seed);
    //arraysort = customArray.clone();
    Arrays.sort(arraysort);
    System.out.println("Arrays are equal: " + Arrays.equals(arraysort, a));


    // MULTICORE

    int[] multiUnsortedArray = Oblig4Precode.generateArray(n, seed);
    //multiUnsortedArray = customArray.clone();

    for (int i = 0; i < NUM_REPETITIONS; i++) {
      long start, stop;
      start = System.nanoTime();
      rs.multiRadixSort(multiUnsortedArray, useBits);
      stop = System.nanoTime();
      parallelTimes[i] = stop - start;
      System.out.println("Parallel time run #" + i + "   : " + parallelTimes[i]);
    }

    Arrays.sort(sequentialTimes);
    Arrays.sort(parallelTimes);
    System.out.println("Median sequential time : " + sequentialTimes[NUM_REPETITIONS / 2]);
    System.out.println("Median parallel time   : " + parallelTimes[NUM_REPETITIONS / 2]);
    System.out.println("Speedup                : " + ((double)sequentialTimes[NUM_REPETITIONS / 2] / (double)parallelTimes[NUM_REPETITIONS / 2]));

    compareArrays(rs.sequentialSorted, rs.multiSorted);


  }

}