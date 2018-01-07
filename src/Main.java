
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 目前只适配1920X1080屏幕
 * 主要思路是：
 * 获取棋子的底座位置和下一个跳盘的位置
 * 棋子底座的位置通过opencv的图形匹配算法来获得，匹配的样本是我剪切出来的棋子照片，见'chess.png'
 * 下一个跳板的位置的话，因为任意相邻两个跳板的中心连线的斜率是相同的，所以我们只要获得
 * 跳板的顶点位置和棋子的底座中心位置的话，利用斜率就可以求出跳板的中心位置
 * 使用勾股定理求两中心之间的距离，乘以系数就是按压的时间
 * 剩下的搜索adb调试android设备
 * 模拟点按操作
 */
public class Main {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    //数量
    private static final int imageLengthLength = 5;

    //存放图片的大小
    private static final long[] imageLength = new long[imageLengthLength];

    //安卓adb截图指令
    private final String[] ADB_SCREEN_CAPTURE_CMDS = {
            "adb shell screencap -p /sdcard/screenshot.png",
            "adb pull /sdcard/screenshot.png ."
    };

    //保存RGB信息的对象
    private RGBInfo rgbInfo = new RGBInfo();

    //按压时间系数，根据屏幕分辨率自行调整
    private final double pressTimeCoefficient = 1.393;

    //按压的起始点坐标，也是再来一局的起始点坐标
    private final int swipeX = 550;
    private final int swipeY = 1580;

    //截屏中得分显示区域最下方的Y坐标，300是1920X1080的值
    private final int gameScoreBottomY = 300;

    //两个跳板出现的中心位置，用来计算角度，获得跳板的Y坐标
    private final int boardX1 = 813;
    private final int boardY1 = 1122;
    private final int boardX2 = 310;
    private final int boardY2 = 813;

    public static void main(String[] args) {
        try {
            int executeCount = 0;
            Main helper = new Main();
            helper.checkDoReplay();
            while (true) {
                helper.executeADBCaptureCommands();
                File currentImage = new File("./screenshot.png");
                if (!currentImage.exists()) {
                    System.out.println("图片不存在");
                    continue;
                }
                long length = currentImage.length();
                imageLength[executeCount % imageLengthLength] = length;
                helper.checkDoReplay();
                executeCount ++;
                System.out.println("当前第" + executeCount + "次执行");
                int[] positions = helper.getBoardAndChessXYValue("./screenshot.png");
                if (positions == null) {
                    System.out.println("坐标为空");
                    continue;
                }
                int chessX = positions[0];
                int chessY = positions[1];
                int boardX = positions[2];
                int boardY = positions[3];
                System.out.println("ChessX: " + chessX + ", ChessY: " + chessY
                        + ", BoardX: " + boardX + ", BoardY: " + boardY);
                double jumpDistance = helper.computeJumDistance(chessX, chessY, boardX, boardY);
                helper.doJump(jumpDistance);
                TimeUnit.MILLISECONDS.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查是否需要重新开局
     */
    private void checkDoReplay() {
        if (imageLength[0] > 0 && imageLength[0] == imageLength[1] && imageLength[1] == imageLength[2]
                && imageLength[2] == imageLength[3] && imageLength[3] == imageLength[4])
        {
            //此时表示已经连续5次图片大小一样了，可知当前屏幕处于再来一局
            Arrays.fill(imageLength, 0);
            //模拟点击再来一局按钮重新开局
            replayGame();
        }
    }
    /**
     * adb获取安卓截屏
     */
    private void executeADBCaptureCommands() {
        for (String command : ADB_SCREEN_CAPTURE_CMDS) {
            executeCommand(command);
        }
    }

    /**
     * 跳一下
     * @param distance
     */
    private void doJump(double distance) {
        System.out.println("distance : " + distance);
        int pressTime = (int) Math.max(distance*pressTimeCoefficient, 200);
        System.out.println("presstime : " + pressTime);
        String command = String.format("adb shell input swipe %s %s %s %s %s",
                swipeX, swipeY, swipeX, swipeY, pressTime);
        System.out.println(command);
        executeCommand(command);
    }

    /**
     * 重新开局
     */
    private void replayGame() {
        String command = String.format("adb shell input tap %s %s", swipeX, swipeY);
        executeCommand(command);
    }

    private double computeJumDistance(int chessX, int chessY, int boardX, int boardY) {
        return Math.sqrt(Math.pow(Math.abs(boardX - chessX), 2) + Math.pow(Math.abs(boardY - chessY), 2));
        //return Math.abs(chessX - boardX);
    }
    /**
     * 执行系统指令
     * @param command
     */
    private void executeCommand(String command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            System.out.println("exec command start : " + command);
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line = br.readLine();
            if (line != null) {
                System.out.println(line);
            }
            System.out.println("exec command end : " + command);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * 获得跳板的中心坐标和棋子的坐标
     * @param currentImage
     * @return
     */
    private int[] getBoardAndChessXYValue(String currentImage) {
        try {
            BufferedImage image = ImageIO.read(new File(currentImage));
            int width = image.getWidth();
            int height = image.getHeight();
            System.out.println("宽度: " + width + ", 高度: " + height);
            int boardX = 0;
            int boardY = 0;
            //获取棋子的XY坐标和棋子的宽度高度
            double[] chessInfo = getChessPoint(Imgcodecs.imread(currentImage), Imgcodecs.imread("./chess.jpg"));
            int chessX = (int)chessInfo[0];
            int chessY = (int)chessInfo[1];
            int chessWidth = (int)chessInfo[2];
            int chessHeight = (int)chessInfo[3];

            //该双重循环用来获得跳板的上顶点
            for (int y = gameScoreBottomY; y < height; y ++) {
                processRGBInfo(image, 0, y);
                int lastPixelR = this.rgbInfo.getRValue();
                int lastPixelG = this.rgbInfo.getGValue();
                int lastPixelB = this.rgbInfo.getBValue();
                /**
                 * 只要boardX>0就表示下个跳板的中心坐标x取到了
                 */
                if (boardX > 0) {
                    break;
                }
                int boardXSum = 0;
                int boardXCount = 0;
                for (int x = 0; x < width; x ++) {
                    processRGBInfo(image, x, y);
                    int pixelR = this.rgbInfo.getRValue();
                    int pixelG = this.rgbInfo.getGValue();
                    int pixelB = this.rgbInfo.getBValue();

                    //棋子头部比下一个跳板还要高
                    if (Math.abs(x - chessX) < chessWidth / 2) {
                        continue;
                    }
                    if ((Math.abs(pixelR - lastPixelR) + Math.abs(pixelG - lastPixelG) + Math.abs(pixelB - lastPixelB)) > 50) {
                        boardXSum += x;
                        boardXCount ++;
                    }
                }
                if (boardXSum > 0) {
                    boardX = boardXSum / boardXCount;
                }
            }
            boardY = chessY - Math.abs(boardX - chessX) *
                    Math.abs(boardY1 - boardY2) / Math.abs(boardX1 - boardX2);
            if (boardX > 0 && boardY > 0) {
                int[] result = new int[]{chessX, chessY, boardX, boardY};
                return result;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 边缘检测
     * @param frame:需要边缘检测处理的图片
     * @return ：只有边缘的图片
     */
    private static Mat doCanny(Mat frame) {
        Mat grayImage = new Mat();
        Mat detectedEdges = new Mat();
        double threshold = 10d;
        Imgproc.cvtColor(frame, grayImage, Imgproc.COLOR_RGB2GRAY);
        //使用3X3内核来降噪
        Imgproc.blur(grayImage, detectedEdges, new Size(3, 3));
        //运行Canny算子
        Imgproc.Canny(detectedEdges, detectedEdges, threshold, threshold*3);
        Mat dest = new Mat();
        frame.copyTo(dest, detectedEdges);
        Imgcodecs.imwrite("./detected.png", detectedEdges);
        System.out.println(detectedEdges);
        return dest;
    }

    /**
     * 通过opencv的匹配算法获取棋子的位置
     * @param source：原图片
     * @param templete: 匹配使用的样图
     * @return : 棋子的坐标
     */
    private double[] getChessPoint(Mat source, Mat templete) {
        Mat result = Mat.zeros(source.rows() - templete.rows() + 1,
                source.cols() - templete.cols() + 1, CvType.CV_32FC1);
        Imgproc.matchTemplate(source, templete, result, Imgproc.TM_SQDIFF_NORMED);
        Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1);
        Core.MinMaxLocResult mlr = Core.minMaxLoc(result);
        org.opencv.core.Point matchLoc = mlr.minLoc;

        System.out.println(matchLoc.x + ":" + matchLoc.y);
        return new double[]{matchLoc.x + templete.width() / 2, matchLoc.y + templete.height(), templete.width(), templete.height()};
    }

    private void detectedChessTest() {
        Mat source, templete;
        source = Imgcodecs.imread("D:/adb/screenshot.png");
        templete = Imgcodecs.imread("D:/adb/chess.png");
        Mat result = Mat.zeros(source.rows() - templete.rows() + 1,
                source.cols() - templete.cols() + 1, CvType.CV_32FC1);
        Imgproc.matchTemplate(source, templete, result, Imgproc.TM_SQDIFF_NORMED);
        Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1);
        Core.MinMaxLocResult mlr = Core.minMaxLoc(result);
        org.opencv.core.Point matchLoc = mlr.minLoc;

        System.out.println(matchLoc.x + ":" + matchLoc.y);
        Imgproc.rectangle(source, matchLoc, new org.opencv.core.Point(matchLoc.x + templete.width(), matchLoc.y + templete.height()), new Scalar(0, 255, 0));

        Imgcodecs.imwrite("D:/adb/result.png", source);
    }

    /**
     * 获取指定位置的RGB值
     * @param bufferedImage
     * @param x
     * @param y
     */
    private void processRGBInfo(BufferedImage bufferedImage, int x, int y) {
        this.rgbInfo.reset();
        int pixel = bufferedImage.getRGB(x, y);
        this.rgbInfo.setRValue((pixel & 0xff0000) >> 16);
        this.rgbInfo.setGValue((pixel & 0xff00) >> 8);
        this.rgbInfo.setBValue(pixel & 0xff);
    }

    /**
     * 记录RGB参数的类
     */
    private class RGBInfo {
        private int RValue;
        private int GValue;
        private int BValue;

        public int getBValue() {
            return BValue;
        }

        public int getGValue() {
            return GValue;
        }

        public int getRValue() {
            return RValue;
        }

        public void setGValue(int GValue) {
            this.GValue = GValue;
        }

        public void setBValue(int BValue) {
            this.BValue = BValue;
        }

        public void setRValue(int RValue) {
            this.RValue = RValue;
        }
        public void reset() {
            this.BValue = 0;
            this.RValue = 0;
            this.GValue = 0;
        }
    }
}