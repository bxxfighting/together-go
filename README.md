# 声明
最近在玩一起来捉妖，挺有意思，但是要出去捉妖，并且捉着太麻烦，我打算写一个辅助功能

# 功能列表
1. 自动识别屏幕上的鼓和妖，并且可以自动完成敲鼓和捉妖  
2. 模拟定位，可以在地图上随意行走  
3. 搜索特定妖灵，实现自动搜索，自动捉妖

# 所需技术
1. 腾讯定位sdk获取当前位置
2. 网易的airtest来实现自动捉妖
3. gwgo接口: wss://publicld.gwgo.qq.com?account_value=0&account_type=1&appid=0&token=0，用来定位妖灵位置
   https://github.com/HubertZhang/gwgo-map
4. 安卓虚拟定位，来实现向妖灵移动


# 已完成功能
1. 模拟定位，控制方向和速度

# 演示效果
<video src="https://github.com/bxxfighting/together-go/blob/master/data/演示.mp4" width="320" height="180" controls="controls">
</video>
