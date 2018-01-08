# Java版微信跳一跳辅助外挂

本文大量代码摘自[JAVA实现微信跳一跳辅助](http://blog.csdn.net/lihushiwoa/article/details/78942322)

## 简介

本文介绍了Java版的微信跳一跳辅助外挂实现过程，使用了openCV的图形匹配代码，目前只匹配1920x1080屏幕，代码见[github-crazyStrome](https://github.com/crazyStrome/WechatJumpHelper)
，仅供学习使用。

![成果](https://cdn-std.dprcdn.net/files/acc_615569/Eg6SRN)

## 如何使用

我来教大家如何使用已经打包好的jar打高分，这个jar只能辅助1920x1080的屏幕。

先去这个地址下载[所需文件](https://github.com/crazyStrome/WechatJumpHelper/tree/master/1920x1080-64%E4%BD%8D),下载该文件夹下的所有文件。
其中的opencv_java331.dll是opencv在64位机器上的本地库，如果你是32位电脑，可以下载opencv的安装包，安装教程如下:[传送门](https://jingyan.baidu.com/article/22a299b5cad4a49e18376a7b.html)
安装完成后，打开.\build\java\x86的文件夹，里面有个opencv_java331.dll就是x86电脑要用的文件。

把这个opencv_java331.dll文件复制到C:\Windows\System32目录下。

运行这个jar文件需要在电脑装上JRE，见如下教程：[传送门](http://blog.csdn.net/u012934325/article/details/73441617)

安装完成后，查看下面的ADB指令相关内容，使得电脑可以调试手机。

双击WechatHelper.bat，脚本开始运行。

## 思路

*  使用adb截取Android设备的屏幕，并上传到电脑上去
*  使用opencv的图形匹配找到棋子的位置，并推算出底座中心坐标
*  对截取得图形进行像素遍历，获得下一个跳板的上顶点坐标
*  因为任意相邻两个跳板中心的连线斜率的绝对值都是一样的，不论是正是负，都可以通过棋子的底座中心坐标（x,y）和下一个跳板的中心x坐标而获得其y坐标
*  通过棋子的坐标和下一个跳板的坐标获得两者的距离
*  距离乘以一个系数为需要按压的时间/ms级
*  使用adb指令模拟手指按压
*  每次检测是否需要重新开局

### ADB指令

什么是[ADB](https://developer.android.com/studio/command-line/adb.html)？我们看一下官方介绍

```
Android Debug Bridge (adb) is a versatile command-line
tool that lets you communicate with a device. The adb 
command facilitates a variety of device actions, such 
as installing and debugging apps, and it provides access 
to a Unix shell that you can use to run a variety of 
commands on a device.
```

其实ADB就是模拟你在手机上的各种操作：手指点按等等，我们这次需要的就是这个。而且，它会提供一个Unix的Shell，从而你可以执行各种只要手机上有而且你有权限的各种指令，比如截屏指令。
ADB指令是Google为Android设备提供的调试工具，只能在Android设备上使用，所以我的代码也只是针对Android设备的。

那么如何使用ADB调试设备呢？再看一下官方介绍

```
To use adb with a device connected over USB, you must enable USB 
debugging in the device system settings, under Developer options.
On Android 4.2 and higher, the Developer options screen is hidden 
by default. To make it visible, go to Settings > About phone and 
tap Build number seven times. Return to the previous screen to find 
Developer options at the bottom.On some devices, the Developer options 
screen might be located or named differently.
You can now connect your device with USB. You can verify that your
device is connected by executing adb devices from the 
android_sdk/platform-tools/ directory. If connected, you'll see 
the device name listed as a "device."
Note: When you connect a device running Android 4.2.2 or higher, 
the system shows a dialog asking whether to accept an RSA key that 
allows debugging through this computer. This security mechanism 
protects user devices because it ensures that USB debugging and 
other adb commands cannot be executed unless you're able to unlock 
the device and acknowledge the dialog.
```
上面那么多就是说，想要调试设备，需要打开手机上开发者选项的USB Debug选项，操作如下。

*  打开如下界面，不同手机不同方式，具体搜索“如何查看Android手机版本号”

![版本号](https://cdn-std.dprcdn.net/files/acc_615569/5AuWJl)

*  点击七次版本号，它会提示你已经打开开发者选项，按下返回键，你会发现多了一条开发者选项，点击进入，启用USB调试

![开发者选项](https://cdn-std.dprcdn.net/files/acc_615569/JrT6aM)

*  用数据线连接电脑和手机，下载[adb工具](https://dl.google.com/android/repository/platform-tools-latest-windows.zip)，解压并添加到系统环境变量中。
*  在Android版本4.2.2以上的系统中会弹出一个对话框询问是否接受一个RSA密钥，点击确认
*  打开cmd，输入如下代码

```
adb devices
```

*  会看到你的设备，就说明操作成功了

有时候没有数据线调试的话，可以使用wifi进行adb调试。懒得贴官方文档了。如下步骤：
*  连接电脑和手机，打开cmd，输入如下命令

```
adb tcpip 5555
```

*  不一定是5555，可以是任何一个端口号
*  断开数据线，将你的手机和电脑置于一个局域网下，假设手机ip为192.168.1.1，输入：

```
adb connect 192.168.1.1:5555
```

*  就可以开启wifi调试了，不信的话输入如下指令：

```
adb reboot
```

*  就重启手机了。

我们需要使用adb指令来截图并传到电脑上，模拟手指的按压和点击，命令如下：

*  adb截图：

```
adb shell screencap -p /sdcard/screenshot.png
```

*  adb将截图上传到电脑:

···
adb pull /sdcard/screenshot.png .
···

* adb模拟手指点击x、y为横纵坐标：

```
adb shell input tap x y
```

* adb模拟按压指令，x、y为横纵坐标，time为按压的时间/ms

```
adb shell input swipe x1 y1 x2 y2 time
```
### openCV图形匹配

在此不再介绍openCV，自行百度。

进行图形匹配，就需要一个匹配用的样本，我用[在线ps](http://www.uupoop.com/)截出一个棋子的图片如下：

![棋子](https://cdn-std.dprcdn.net/files/acc_615569/s11hB3)

我并没有完全截取整个图形，目的是让现在的图形底部正好和棋子的底座中心位置重合，这样便于以后的计算。匹配测试的代码如下，我写了一个方法来测试：

```
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
```
我使用如下图片进行测试：

![测试图片](https://cdn-std.dprcdn.net/files/acc_615569/eymS4W)

检测结果如下，可以看到在棋子周围有一个绿色框子:

![检测结果](https://cdn-std.dprcdn.net/files/acc_615569/LbO7Fe)

我实现了一个方法，来获取棋子的中心坐标：

```
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
```

### 对图形遍历以便获得下一个跳板的上顶点坐标

原作者使用遍历来获取棋子和跳板的坐标，而我只是用遍历来获取跳板的坐标，如下图：

![跳板顶点](https://cdn-std.dprcdn.net/files/acc_615569/SbJ5tD)

可见绿色跳板和背景色差别很大，作者可以根据这个来进行区分，但是分数栏的颜色也很显眼，作者就从分数栏以下开始遍历像素点，即y从300开始。

```
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
		//获取背景色
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
		    //判断背景色和当前RGB是否有很大差异
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
```

### 通过上顶点坐标获取跳板中心坐标

我们看两张图片：

![1](https://cdn-std.dprcdn.net/files/acc_615569/krWoWp)

![2](https://cdn-std.dprcdn.net/files/acc_615569/wNG3Zq)

可以看到，两条线的斜率绝对值是差不多相等的，现在知道了中心的横坐标，就很容易根据斜率求得y坐标：

```
boardY = chessY - Math.abs(boardX - chessX) *
   Math.abs(boardY1 - boardY2) / Math.abs(boardX1 - boardX2);
```

其中：

```
    //两个跳板出现的中心位置，用来计算角度，获得跳板的Y坐标
    private final int boardX1 = 813;
    private final int boardY1 = 1122;
    private final int boardX2 = 310;
    private final int boardY2 = 813;
```

自己可以在图片中获得。

### 通过坐标获取距离，然后获取按压时间

勾股定理获取距离：

```
    private double computeJumDistance(int chessX, int chessY, int boardX, int boardY) {
        return Math.sqrt(Math.pow(Math.abs(boardX - chessX), 2) + Math.pow(Math.abs(boardY - chessY), 2));
        //return Math.abs(chessX - boardX);
    }
```

很简单。

距离再乘以一个系数：

```
    //按压时间系数，根据屏幕分辨率自行调整
    private final double pressTimeCoefficient = 1.393;
```

这是1920x1080的按压系数，其他分辨率的按压系数以及分数栏右下坐标值可以看这个python项目，里面有众多网友的总结，在config文件夹下，[传送门](https://github.com/wangshub/wechat_jump_game)

### 检测是否需要重新开局

原作者定义了一个大小为5的数组，每次跳一下之后，都会将当前图片大小储存到数组中去，然后进行判断，如果数组的五个元素都相等的话说明画面已经很久没有变化了，也就是说明已经结束了，可以再次开局了。

```
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
```

### 再次感谢原作者