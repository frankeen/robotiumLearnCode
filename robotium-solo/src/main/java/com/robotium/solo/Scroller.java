package com.robotium.solo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.robotium.solo.Solo.Config;
import junit.framework.Assert;
import android.app.Instrumentation;
import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ScrollView;


/**
 * 滚动条操作工具类
 * Contains scroll methods. Examples are scrollDown(), scrollUpList(),
 * scrollToSide().
 *
 * @author Renas Reda, renas.reda@robotium.com
 *
 */

class Scroller {
  	// 向下
	public static final int DOWN = 0;
	// 向上
	public static final int UP = 1;
	// 左右 枚举
	public enum Side {LEFT, RIGHT}
	// 是否可以拖动
	private boolean canScroll = false;
	// Instrument对象
	private final Instrumentation inst;
	//  View获取工具类
	private final ViewFetcher viewFetcher;
	// 延时工具类
	private final Sleeper sleeper;
	// Robotium属性配置类
	private final Config config;


	/**
	 * Constructs this object.
	 *
	 * @param inst the {@code Instrumentation} instance
	 * @param viewFetcher the {@code ViewFetcher} instance
	 * @param sleeper the {@code Sleeper} instance
	 */

	public Scroller(Config config, Instrumentation inst, ViewFetcher viewFetcher, Sleeper sleeper) {
		this.config = config;
		this.inst = inst;
		this.viewFetcher = viewFetcher;
		this.sleeper = sleeper;
	}


	/**
	 * 按住并且拖动到指定位置
	 * fromX 起始X坐标
	 * toX 终点X坐标
	 * fromY 起始Y坐标
	 * toY 终点Y坐标
	 * stepCount 动作拆分成几步
	 * Simulate touching a specific location and dragging to a new location.
	 *
	 * This method was copied from {@code TouchUtils.java} in the Android Open Source Project, and modified here.
	 *
	 * @param fromX X coordinate of the initial touch, in screen coordinates
	 * @param toX Xcoordinate of the drag destination, in screen coordinates
	 * @param fromY X coordinate of the initial touch, in screen coordinates
	 * @param toY Y coordinate of the drag destination, in screen coordinates
	 * @param stepCount How many move steps to include in the drag
	 */

	public void drag(float fromX, float toX, float fromY, float toY,
			int stepCount) {
		// 获取当前系统时间，构造MontionEvent使用
		long downTime = SystemClock.uptimeMillis();
		// 获取当前系统时间，构造MontionEvent使用
		long eventTime = SystemClock.uptimeMillis();
		float y = fromY;
		float x = fromX;
		// 计算每次增加Y坐标量
		float yStep = (toY - fromY) / stepCount;
		// 计算每次增加X坐标量
		float xStep = (toX - fromX) / stepCount;
		// 构造MotionEvent,先按住
		MotionEvent event = MotionEvent.obtain(downTime, eventTime,MotionEvent.ACTION_DOWN, fromX, fromY, 0);
		try {
			// 通过Instrument发送按住事件
			inst.sendPointerSync(event);
			// 抓取可能出现的异常
		} catch (SecurityException ignored) {}
		// 按照设置的步数，发送Move事件
		for (int i = 0; i < stepCount; ++i) {
			y += yStep;
			x += xStep;
			eventTime = SystemClock.uptimeMillis();
			// 构造 MOVE事件
			event = MotionEvent.obtain(downTime, eventTime,MotionEvent.ACTION_MOVE, x, y, 0);
			try {
				// 通过Instrument发送按住事件
				inst.sendPointerSync(event);
				// 抓取可能出现的异常
			} catch (SecurityException ignored) {}
		}
		// 获取系统当前时间
		eventTime = SystemClock.uptimeMillis();
		// 构造松开事件
		event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP,toX, toY, 0);
		try {

			inst.sendPointerSync(event);
		} catch (SecurityException ignored) {}
	}


	/**
	 * 按照设定的方法拖动滚动条,已经处于顶部的，调用拖动到顶部无效
	 * view 带滚动条的View控件
	 * direction 拖动方向 0 滚动条向上拉动,1滚动条向下拉动
	 * Scrolls a ScrollView.
	 *
	 * @param direction the direction to be scrolled
	 * @return {@code true} if scrolling occurred, false if it did not
	 */

	public boolean scrollView(final View view, int direction){
		// null 检查，比较传入null参数引发异常
		if(view == null){
			return false;
		}
		// 获取控件的高度
		int height = view.getHeight();
		// 高度减小一个像素
		height--;
		int scrollTo = -1;
		// 向上拉动，设置成滚动条的高度,拉到顶部
		if (direction == DOWN) {
			scrollTo = height;
		}
		// 向下拉动，设置成负值,拉到底部
		else if (direction == UP) {
			scrollTo = -height;
		}
		// 获取当前滚动的高度位置
		int originalY = view.getScrollY();
		final int scrollAmount = scrollTo;
		inst.runOnMainSync(new Runnable(){
			public void run(){
				view.scrollBy(0, scrollAmount);
			}
		});
		// 滚动条坐标未变化，标识本次拖动动作失败.已经处于顶端了，触发无效果
		if (originalY == view.getScrollY()) {
			return false;
		}
		else{
			return true;
		}
	}

	/**
	 * 滚动条滑到底部或者顶部，已经处于顶部，调用该方法拖动到顶部将引发死循环
	 * Scrolls a ScrollView to top or bottom.
	 *
	 * @param direction the direction to be scrolled
	 */

	public void scrollViewAllTheWay(final View view, final int direction) {
		while(scrollView(view, direction));
	}

	/**
	 * 拖动到顶部或者底部,0拖动到顶部,1拖动到底部
	 * Scrolls up or down.
	 *
	 * @param direction the direction in which to scroll
	 * @return {@code true} if more scrolling can be done
	 */

	public boolean scroll(int direction) {
		return scroll(direction, false);
	}

	/**
	 * 拖动到顶部
	 * Scrolls down.
	 *
	 * @return {@code true} if more scrolling can be done
	 */

	public boolean scrollDown() {
		// 如果配置设置了禁止拖动，那么将不拖动控件
		if(!config.shouldScroll) {
			return false;
		}
		// 拖动到顶部
		return scroll(Scroller.DOWN);
	}

	/**
	 * 拖动当前页面的可拖动控件
	 * direction 0拖动到顶部,1拖动到底部
	 * Scrolls up and down.
	 *
	 * @param direction the direction in which to scroll
	 * @param allTheWay <code>true</code> if the view should be scrolled to the beginning or end,
	 *                  <code>false</code> to scroll one page up or down.
	 * @return {@code true} if more scrolling can be done
	 */

	@SuppressWarnings("unchecked")
	public boolean scroll(int direction, boolean allTheWay) {
		// 获取所有的Clicker可操作Views
		ArrayList<View> viewList = RobotiumUtils.
				removeInvisibleViews(viewFetcher.getAllViews(true));
		// 获取所有可以拖动操作的views
		ArrayList<View> views = RobotiumUtils.filterViewsToSet(new Class[] { ListView.class,
				ScrollView.class, GridView.class, WebView.class}, viewList);
		// 获取所有可视view中的最新的，即当前用户选中的可拖动控件
		View view = viewFetcher.getFreshestView(views);
		
		ArrayList<View> viewsToCheck = new ArrayList<View>();
		viewsToCheck.add(view);
		
		List<View> recyclerViews = viewFetcher.getAllRecyclerViews(true);
		
		for(View viewToScroll : recyclerViews){
			viewsToCheck.add(viewToScroll);
		}
	
		view = viewFetcher.getFreshestView(viewsToCheck);
		
		// 如果无可拖动控件，则返回
		if (view == null) {
				return false;
		}
		// 是一个列表控件，则使用列表控件方法操作
		if (view instanceof AbsListView) {
			return scrollList((AbsListView)view, direction, allTheWay);
		}
		// 如果是一个WebView控件，则按照WebView方法操作
		if(view instanceof WebView){
			return scrollWebView((WebView)view, direction, allTheWay);
		}

		if (allTheWay) {
			scrollViewAllTheWay(view, direction);
			return false;
		} else {
			return scrollView(view, direction);
		}
	}
	
	/**
	 * WebView 控件 拖动操作.
	 * webView 传入的WebView
	 * direction 操作方向，0拖动到顶部，1拖动到底部
	 * allTheWay true标识拖动到底部或顶部，false标识不拖动
	 * 事件发送成功返回true 失败返回false
	 *
	 * Scrolls a WebView.
	 * 
	 * @param webView the WebView to scroll
	 * @param direction the direction to scroll
	 * @param allTheWay {@code true} to scroll the view all the way up or down, {@code false} to scroll one page up or down                          or down.
	 * @return {@code true} if more scrolling can be done
	 */
	
	public boolean scrollWebView(final WebView webView, int direction, final boolean allTheWay){
		// 调用Instrument发送拖动事件
		if (direction == DOWN) {
			inst.runOnMainSync(new Runnable(){
				public void run(){
					// 拖动到底部
					canScroll =  webView.pageDown(allTheWay);
				}
			});
		}
		// 调用Instrument发送拖动事件
		if(direction == UP){
			inst.runOnMainSync(new Runnable(){
				public void run(){
					canScroll =  webView.pageUp(allTheWay);
				}
			});
		}
		// 返回事件发送是否成功
		return canScroll;
	}

	/**
	 * 拖动一个列表
	 * absListView AbsListView类型的，即列表类控件
	 * direction 拖动方向0最顶部，1最底部
	 * Scrolls a list.
	 *
	 * @param absListView the list to be scrolled
	 * @param direction the direction to be scrolled
	 * @param allTheWay {@code true} to scroll the view all the way up or down, {@code false} to scroll one page up or down
	 * @return {@code true} if more scrolling can be done
	 */

	public <T extends AbsListView> boolean scrollList(T absListView, int direction, boolean allTheWay) {
		// 非null校验
		if(absListView == null){
			return false;
		}
		// 拖动到底部
		if (direction == DOWN) {
			
			int listCount = absListView.getCount();
			int lastVisiblePosition = absListView.getLastVisiblePosition();
			// 如果是直接拖动到底部的模式
			if (allTheWay) {
				scrollListToLine(absListView, listCount-1);
				return false;
			}
			if (lastVisiblePosition >= listCount - 1) {
				if(lastVisiblePosition > 0){
					scrollListToLine(absListView, lastVisiblePosition);
				}
				return false;
			}
			
			int firstVisiblePosition = absListView.getFirstVisiblePosition();
			
			// 当不是一行时，拖动到最下面的行
			if(firstVisiblePosition != lastVisiblePosition)
				scrollListToLine(absListView, lastVisiblePosition);

			else
				scrollListToLine(absListView, firstVisiblePosition + 1);

		} else if (direction == UP) {
			int firstVisiblePosition = absListView.getFirstVisiblePosition();
			
			if (allTheWay || firstVisiblePosition < 2) {
				scrollListToLine(absListView, 0);
				return false;
			}
			int lastVisiblePosition = absListView.getLastVisiblePosition();

			final int lines = lastVisiblePosition - firstVisiblePosition;
			
			int lineToScrollTo = firstVisiblePosition - lines;

			if(lineToScrollTo == lastVisiblePosition)
				lineToScrollTo--;

			if(lineToScrollTo < 0)
				lineToScrollTo = 0;

			scrollListToLine(absListView, lineToScrollTo);
		}
		sleeper.sleep();
		return true;
	}


	/**
   	 * 拖动列表内容到指定的行
     * line 对应的行号
	 * Scroll the list to a given line
	 *
	 * @param view the {@link AbsListView} to scroll
	 * @param line the line to scroll to
	 */

	public <T extends AbsListView> void scrollListToLine(final T view, final int line){
		// 非null校验
		if(view == null)
			Assert.fail("AbsListView is null!");

		final int lineToMoveTo;
		// 如果是gridview类型的，带标题，因此行数+1
		if(view instanceof GridView) {
			lineToMoveTo = line+1;
		}
		else {
			lineToMoveTo = line;
		}
		// 发送拖动事件
		inst.runOnMainSync(new Runnable(){
			public void run(){
				view.setSelection(lineToMoveTo);
			}
		});
	}


	/**
	 * 横向拖动,拖动默认拆分成40步操作
  	 * side 指定拖动方向
	 * scrollPosition 拖动百分比0-1.
	 * Scrolls horizontally.
	 *
	 * @param side the side to which to scroll; {@link Side#RIGHT} or {@link Side#LEFT}
	 * @param scrollPosition the position to scroll to, from 0 to 1 where 1 is all the way. Example is: 0.55.
	 * @param stepCount how many move steps to include in the scroll. Less steps results in a faster scroll
	 */

	@SuppressWarnings("deprecation")
	public void scrollToSide(Side side, float scrollPosition, int stepCount) {
		WindowManager windowManager = (WindowManager) 
				inst.getTargetContext().getSystemService(Context.WINDOW_SERVICE);
		// 获取屏幕高度
		int screenHeight = windowManager.getDefaultDisplay()
				.getHeight();
		// 获取屏幕宽度
		int screenWidth = windowManager.getDefaultDisplay()
				.getWidth();
		// 按照宽度计算总距离
		float x = screenWidth * scrollPosition;
		// 拖动选择屏幕正中间
		float y = screenHeight / 2.0f;
		//往左拖动
		if (side == Side.LEFT)
			drag(70, x, y, y, stepCount);
		// 往右拖动
		else if (side == Side.RIGHT)
			drag(x, 0, y, y, stepCount);
	}

	/**
	 * 对给定控件进行向左或向右拖动操作.默认拖动距离拆分成40步
	 * view 需要拖动操作的控件
	 * side 拖动方向
	 * scrollPosition 拖动距离，按照屏幕宽度百分比计算，值为0-1
	 * Scrolls view horizontally.
	 *
	 * @param view the view to scroll
	 * @param side the side to which to scroll; {@link Side#RIGHT} or {@link Side#LEFT}
	 * @param scrollPosition the position to scroll to, from 0 to 1 where 1 is all the way. Example is: 0.55.
	 * @param stepCount how many move steps to include in the scroll. Less steps results in a faster scroll
	 */

	public void scrollViewToSide(View view, Side side, float scrollPosition, int stepCount) {
		// 临时变量，存储控件在手机屏幕中的相对坐标
		int[] corners = new int[2];
		// 获取相对坐标
		view.getLocationOnScreen(corners);
		// 获取高度相对坐标
		int viewHeight = view.getHeight();
		// 获取宽度相对坐标
		int viewWidth = view.getWidth();
		// 计算拖动开始x坐标
		float x = corners[0] + viewWidth * scrollPosition;
		// 计算拖动开始y坐标
		float y = corners[1] + viewHeight / 2.0f;
		// 往左拖动
		if (side == Side.LEFT)
			drag(corners[0], x, y, y, stepCount);
		else if (side == Side.RIGHT)
			drag(x, corners[0], y, y, stepCount);
	}
}
