package cn.intret.app.picgo.utils;

/**
 * 参数
 */
public final class Param {

    /**
     * 保存2个参数的数据结构。
     */
    public static final class Two<Type1, Type2> {

        public Type1 p1;
        public Type2 p2;

        public Two() {

        }

        public Two(Type1 data1, Type2 data2) {
            p1 = data1;
            p2 = data2;
        }

        @Override
        public String toString() {
            return "Two{" +
                    "p1=" + p1 +
                    ", p2=" + p2 +
                    '}';
        }
    }

    /**
     * 包含3个参数的数据结构。
     */
    public static final class Three<Type1, Type2, Type3> {

        public Type1 p1;
        public Type2 p2;
        public Type3 p3;

        public Three() {

        }

        public Three(Type1 data1, Type2 data2, Type3 data3) {
            p1 = data1;
            p2 = data2;
            p3 = data3;
        }

        @Override
        public String toString() {
            return "Three{" +
                    "p1=" + p1 +
                    ", p2=" + p2 +
                    ", p3=" + p3 +
                    '}';
        }
    }

    /**
     * 保存4个参数的数据结构。
     */
    public static final class Four<Type1, Type2, Type3, Type4> {

        public Type1 p1;
        public Type2 p2;
        public Type3 p3;
        public Type4 p4;

        public Four() {

        }

        public Four(Type1 data1, Type2 data2, Type3 data3, Type4 data4) {
            p1 = data1;
            p2 = data2;
            p3 = data3;
            p4 = data4;
        }

        @Override
        public String toString() {
            return "Four{" +
                    "p1=" + p1 +
                    ", p2=" + p2 +
                    ", p3=" + p3 +
                    ", p4=" + p4 +
                    '}';
        }
    }

    /**
     * 保存5个参数的数据结构。
     */
    public static final class Five<Type1, Type2, Type3, Type4, Type5> {

        public Type1 p1;
        public Type2 p2;
        public Type3 p3;
        public Type4 p4;
        public Type5 p5;

        public Five() {

        }

        public Five(Type1 data1, Type2 data2, Type3 data3, Type4 data4, Type5 data5) {
            p1 = data1;
            p2 = data2;
            p3 = data3;
            p4 = data4;
            p5 = data5;
        }

        @Override
        public String toString() {
            return "Five{" +
                    "p1=" + p1 +
                    ", p2=" + p2 +
                    ", p3=" + p3 +
                    ", p4=" + p4 +
                    ", p5=" + p5 +
                    '}';
        }
    }

    /**
     * 保存6个参数的数据结构。
     */
    public static final class Six<Type1, Type2, Type3, Type4, Type5, Type6> {

        public Type1 p1;
        public Type2 p2;
        public Type3 p3;
        public Type4 p4;
        public Type5 p5;
        public Type6 p6;

        public Six() {

        }

        public Six(Type1 data1, Type2 data2, Type3 data3, Type4 data4, Type5 data5, Type6 data6) {
            p1 = data1;
            p2 = data2;
            p3 = data3;
            p4 = data4;
            p5 = data5;
            p6 = data6;
        }

        @Override
        public String toString() {
            return "Six{" +
                    "p1=" + p1 +
                    ", p2=" + p2 +
                    ", p3=" + p3 +
                    ", p4=" + p4 +
                    ", p5=" + p5 +
                    ", p6=" + p6 +
                    '}';
        }
    }
}
