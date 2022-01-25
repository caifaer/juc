package com.smj.testboot.juc.positive.profile;


/**
 * 其实就是 分治思想   跟fork-join其为相似
 *
 *
 * 分完之后 通过 sum方法 进行累加即可;
 *
 *
 * public long sum() {
 *         Cell[] as = cells; Cell a;
 *         long sum = base;
 *         if (as != null) {
 *             for (int i = 0; i < as.length; ++i) {
 *                 if ((a = as[i]) != null)
 *                     sum += a.value;
 *             }
 *         }
 *         return sum;
 *     }
 *
 *
 *
 *
 *
 */

public class LongAddrSource  {


    /**
     * 在并发量较低的环境下，线程冲突的概率比较小，自旋的次数不会很多。
     * 但是，高并发环境下，N个线程同时进行自旋操作，会出现大量失败并不断自旋的情况，此时AtomicLong的自旋会成为瓶颈。
     * 这就是LongAdder引入的初衷——解决高并发环境下AtomicLong的自旋瓶颈问题。
     *
     * LongAdder的原理是: 在最初无竞争时，只更新base的值，
     *                    当有多线程竞争时通过分段的思想，让不同的线程更新不同的段，
     *                    最后把这些段相加就得到了完整的LongAdder存储的值。
     *  对比一下 AtomicLong
     *      AtomicLong 是所有线程争夺一个 资源。 也就是 一个value变量
     *      LongAdder  则是进行分段  不同的线程负责不同的资源   也就是将value进行拆分，即cell数组。这样获取最终值得直接累加所有cell即可。
     */

    /*
     // 累加方法，参数x为累加的值
     public void add(long x) {

        Cell[] as; long b, v; int m; Cell a;

                * 如果一下两种条件则继续执行if内的语句
                * 1. cells数组是懒加载形式得, 只有存在竞争得时候才会创建。所以cells数组不为null，就意味着没有竞争，此时对base进行cas操作
                * 2. 如果cells数组为null，则执行casBase，就是简单得通过UNSAFE类的cas设置成员变量base的值为base+要累加的值。
                *                   成功，则直接返回(在无竞争的情况下是类似于AtomticInteger处理方式)
                *                   失败  说明第一次争用冲突产生，需要对cells数组初始化，进入if内；
        if ((as = cells) != null || !casBase(b = base, b + x)) {

         // uncontended判断cells数组中，当前线程要做cas累加操作的某个元素是否不存在争用，如果cas失败则存在争用；
         // uncontended=false代表存在争用，uncontended=true代表不存在争用。
         boolean uncontended = true;

         * 1. as == null:  cells数组未被初始化，成立则直接进入if执行cell初始化
         * 2. (m = as.length - 1) < 0： cells数组的长度为0
         *          条件1与2都代表cells数组没有被初始化成功，初始化成功的cells数组长度为2.
         * 3. (a = as[getProbe() & m]) == null  ---> 这里可以简单的理解为当前线程有没有对应的cell被创建.
         *           如果cells被初始化，且它的长度不为0，则通过getProbe方法获取当前线程Thread的threadLocalRandomProbe变量的值，初始为0，然后执行threadLocalRandomProbe&(cells.length-1 ),相当于m%cells.length;如果cells[threadLocalRandomProbe%cells.length]的位置为null，这说明这个位置从来没有线程做过累加，需要进入if继续执行，在这个位置创建一个新的Cell对象；
         * 4. !(uncontended = a.cas(v = a.value, v + x))：尝试对cells[threadLocalRandomProbe%cells.length]位置的Cell对象中的value值做累加操作,并返回操作结果,如果失败了则进入if，重新计算一个threadLocalRandomProbe；

         如果进入if语句执行longAccumulate方法,有三种情况
            1. 前两个条件代表cells没有初始化，
            2. 第三个条件指当前线程hash到的cells数组中的位置还没有其它线程做过累加操作，
            3. 第四个条件代表 该线程有对应得cell对象  但是cas得时候产生了冲突,uncontended=false
        if (as == null || (m = as.length - 1) < 0 ||
                (a = as[getProbe() & m]) == null ||
                !(uncontended = a.cas(v = a.value, v + x)))
            longAccumulate(x, null, uncontended);
    }
}




    final void longAccumulate(long x, LongBinaryOperator fn, boolean wasUncontended) {
        //获取当前线程的threadLocalRandomProbe值作为hash值,如果当前线程的threadLocalRandomProbe为0，说明当前线程是第一次进入该方法，则强制设置线程的threadLocalRandomProbe为ThreadLocalRandom类的成员静态私有变量probeGenerator的值，后面会详细将hash值的生成;
        //另外需要注意，如果threadLocalRandomProbe=0，代表新的线程开始参与cell争用的情况
        //1.当前线程之前还没有参与过cells争用（也许cells数组还没初始化，进到当前方法来就是为了初始化cells数组后争用的）,是第一次执行base的cas累加操作失败；
        //2.或者是在执行add方法时，对cells某个位置的Cell的cas操作第一次失败，则将wasUncontended设置为false，那么这里会将其重新置为true；第一次执行操作失败；
        //凡是参与了cell争用操作的线程threadLocalRandomProbe都不为0；
        int h;
        if ((h = getProbe()) == 0) {
            //初始化ThreadLocalRandom;
            ThreadLocalRandom.current(); // force initialization
            //将h设置为0x9e3779b9
            h = getProbe();
            //设置未竞争标记为true
            wasUncontended = true;
        }
        //    cas冲突标志，表示当前线程hash到的Cells数组的位置，做cas累加操作时与其它线程发生了冲突，cas失败；
        //    collide=true代表有冲突，collide=false代表无冲突
        boolean collide = false;
        for (; ; ) {
            Cell[] as;   Cell a;   int n;  long v;
            //这个主干if有三个分支
            //1.主分支一：cells数组已经正常初始化（这个if分支处理add方法的四个条件中的3和4）
            //2.主分支二：cells数组没有初始化或者长度为0的情况；（这个分支处理add方法的四个条件中的1和2）
            //3.主分支三：cell数组没有初始化，并且其它线程正在执行对cells数组初始化的操作，及cellbusy=1；则尝试将累加值通过cas累加到base上

            // 主分支一
            if ((as = cells) != null && (n = as.length) > 0) {
                //  创建累加单元 也就是数组中的cell对象
                 *内部小分支一：这个是处理add方法内部if分支的条件3：
                 * 如果被hash到的位置为null，(也就是说这个线程 这个位置上没有累加单元 即cells存在  但是没有cell对象)
                 * 说明没有线程在这个位置设置过值，没有竞争，可以直接使用，则用x值作为初始值创建一个新的Cell对象，对cells数组使用cellsBusy加锁，然后将这个Cell对象放到cells[m%cells.length]位置上
                if ((a = as[(n - 1) & h]) == null) {
                    //cellsBusy == 0 --- 未加锁  代表当前没有线程对cells数组做修改
                    if (cellsBusy == 0) {
                        //将要累加的x值作为初始值创建一个新的Cell对象， 还未放到数组中
                        Cell r = new Cell(x);
                        //如果cellsBusy=0无锁(无竞争)，则通过cas将cellsBusy设置为1加锁
                        if (cellsBusy == 0 && casCellsBusy()) { // 加锁 若失败 -- 101行
                            //标记Cell是否创建成功并放入到cells数组被hash的位置上
                            boolean created = false;
                            try {
                                Cell[] rs;
                                int m, j;
                                // 再次检查 cells数组不为null，且长度不为空，且hash到的位置的Cell为null
                                // 否则就是 有其他线程创建了
                                if ((rs = cells) != null &&
                                        (m = rs.length) > 0 &&
                                        rs[j = (m - 1) & h] == null) {
                                    //将新的cell设置到该位置  放到数组中
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                //去掉锁
                                cellsBusy = 0;
                            }
                            //生成成功，跳出循环
                            if (created)
                                break;
                            //如果created为false，说明上面指定的cells数组的位置cells[m % cells.length]已经有其它线程设置了cell了，继续执行循环。
                            // 下一次循环就不会进来了
                            continue;
                        }
                    }
                    //如果执行的当前行，代表cellsBusy=1，有线程正在更改cells数组，代表产生了冲突，将collide设置为false
                    collide = false;

                *内部小分支二：如果add方法中条件4的通过cas设置cells[m%cells.length]位置的Cell对象中的value值设置为v+x失败,说明已经发生竞争，
                * 此时将wasUncontended设置为true，跳出内部的if判断，最后重新计算一个新的probe，然后重新执行循环;
                * // 进到这里意味着  有cells数组  有对应cell对象  但是更新失败了  说明其他线程也在更新 有冲突了
                } else if (!wasUncontended)
                    //设置未竞争标志位true，继续执行，后面会算一个新的probe值，然后重新执行循环。
                    wasUncontended = true;

                *内部小分支三：新的争用线程参与争用的情况：处理刚进入当前方法时threadLocalRandomProbe=0的情况，也就是当前线程第一次参与cell争用的cas失败，这里会尝试将x值加到cells[m%cells.length]的value ，如果成功直接退出
                else if (a.cas(v = a.value, ((fn == null) ? v + x :
                        fn.applyAsLong(v, x))))
                    break;

               *内部小分支四：分支3处理新的线程争用执行失败了，这时如果cells数组的长度已经到了最大值（大于等于cup数量），或者是当前cells已经做了扩容，则将collide设置为false，后面重新计算prob的值
               else if (n >= NCPU || cells != as)
                    collide = false;

                *内部小分支五：如果发生了冲突collide=false，则设置其为true；会在最后重新计算hash值后，进入下一次for循环
                else if (!collide)
                    //设置冲突标志，表示发生了冲突，需要再次生成hash，重试。 如果下次重试任然走到了改分支此时collide=true，!collide条件不成立，则走后一个分支 进行扩容
                    collide = true;

                *内部小分支六：扩容cells数组，新参与cell争用的线程两次均失败，且符合库容条件，会执行该分支
                else if (cellsBusy == 0 && casCellsBusy()) {
                    try {
                        //检查cells是否已经被扩容
                        if (cells == as) {      // Expand table unless stale
                            Cell[] rs = new Cell[n << 1]; // 扩大一倍
                            for (int i = 0; i < n; ++i)
                                rs[i] = as[i];
                            cells = rs;
                        }
                    } finally {
                        cellsBusy = 0;
                    }
                    collide = false;
                    continue;                   // Retry with expanded table
                }
                //为当前线程重新计算hash值   换一个累加单元继续累加
                h = advanceProbe(h);

//这个大的分支处理add方法中的条件1与条件2成立的情况，如果cell表还未初始化或者长度为0，先尝试获取cellsBusy锁。
                                cells == as  判断cells是否被修改了   也就是说是否有其他线程在创建
                                casCellsBusy加锁 将cellsBusy由0变为1
            } else if (cellsBusy == 0 && cells == as && casCellsBusy()) {
                // 此时 就加锁成功
                boolean init = false;
                try {
                    // Initialize table
                    //初始化cells数组，初始容量为2,并将x值通过hash&1，放到0个或第1个位置上
                    if (cells == as) {
                        Cell[] rs = new Cell[2];
                        rs[h & 1] = new Cell(x);
                        cells = rs;
                        init = true;
                    }
                } finally {
                    //解锁
                    cellsBusy = 0;
                }
                //如果init为true说明初始化成功，跳出循环
                if (init)
                    break;
            }
*如果以上操作都失败了，则尝试将值累加到base上 然后break;   如果失败了 继续循环
            else if (casBase(v = base, ((fn == null) ? v + x : fn.applyAsLong(v, x)))) // Fall back on using base
                break;
        }
    }


hash生成策略
hash决定了当前线程将累加值定位到哪个cell中，hash算法尤其重要。
hash就是java的Thread类里面有一个成员变量，初始值为0。

@sun.misc.Contended("tlr")
int threadLocalRandomProbe;

// LongAdder的父类Striped64里通过getProbe方法获取当前线程threadLocalRandomProbe
    static final int getProbe() {
        // PROBE是threadLocalRandomProbe变量在Thread类里面的偏移量
        return UNSAFE.getInt(Thread.currentThread(), PROBE);
    }

// threadLocalRandomProbe初始化
// 线程对LongAdder的累加操作，在没有进入longAccumulate方法前，threadLocalRandomProbe一直都是0，当发生争用后才会进入longAccumulate方法中，进入该方法第一件事就是判断threadLocalRandomProbe是否为0，如果为0，则将其设置为0x9e3779b9
    int h;
    if ((h = getProbe()) == 0) {
       ThreadLocalRandom.current();
       h = getProbe();
       //设置未竞争标记为true
       wasUncontended = true;
    }

    static final void localInit() {
       // private static final AtomicInteger probeGenerator = new AtomicInteger();
       // private static final int PROBE_INCREMENT = 0x9e3779b9;
       int p = probeGenerator.addAndGet(PROBE_INCREMENT);
       int probe = (p == 0) ? 1 : p; // skip 0
       long seed = mix64(seeder.getAndAdd(SEEDER_INCREMENT));
       Thread t = Thread.currentThread();
       UNSAFE.putLong(t, SEED, seed);
       UNSAFE.putInt(t, PROBE, probe);
    }

    threadLocalRandomProbe重新生成
    static final int advanceProbe(int probe) {
       probe ^= probe << 13;   // xorshift
       probe ^= probe >>> 17;
       probe ^= probe << 5;
       UNSAFE.putInt(Thread.currentThread(), PROBE, probe);
       return probe;
    }



 */
}
