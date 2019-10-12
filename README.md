# 声明
最近在玩一起来捉妖，挺有意思，基于此，以学习练手为目的，开发此项目  

本项目谨供学习交流使用，严禁用于任何商业目的，或破坏游戏环境及平衡等行为，如造成任何损失，请自行承担后果  

> 本项目专为安卓开发、测试学习交流使用  
> 如果体验本软件，需要先打开手机的开发者模式(请自行百度)  
> 本人2019年8月1日入坑，发现很好玩，但是游戏本身已经过气的感觉，身边没有任何人玩此游戏  
> 欢迎Star，欢迎Fork，欢迎帮忙分模块改进  


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
2. 自动移动到筛选的妖灵附近


# 使用说明  
由于没有做无权限提醒，所以安装完成后，自己去应用管理中，把所有权限都给上，要不然打开就崩溃 ^ ^  

收/开: 控制悬浮窗大小  

#### 手动控制移动相关  
走/停: 控制人物是否移动  
转盘: 控制人物移动的方向  
拉动条: 控制移动的速度  

#### 快速定位妖灵相关  
妖/鼓/擂/石：切换到妖模式  
筛: 选择想要在地图上查找的妖灵  
搜: 搜索当前位置妖灵  
下：移动到下一个妖灵位置  
图：搜索点击位置妖灵，可以点击搜到的妖灵，移动过去  

#### 快速敲鼓相关  
妖/鼓/擂/石：切换到鼓模式  
图: 移动到点击位置  
存: 记录当前位置  
下: 移动到下一个存储的位置  

#### 擂/石模式，操作同鼓模式  

#### 其它  
T: 获取token，可以不用，目前token不稳定，可以自行去抓取捉妖雷达的token填入到软件中  


# 互动交流
请先Star后加群，进群申请输入：净化游戏环境从我做起，坚决不使用外挂等破坏游戏平衡
QQ群：687364516
> 最新的版本更新会在这里发布  


# 鸣谢(参考资料)
虚拟定位: 
* https://github.com/Hilaver/MockGPS
* https://github.com/Aslanchen/SimulateGps

捉妖雷达:
* https://github.com/liuzirui1122/zhuoyao_radar


# 捉妖一个月成果展示(按资质排序)  
<img display="inline-block" src="https://github.com/bxxfighting/together-go/blob/master/data/pets/WechatIMG451.jpeg" width="196" hegiht="400" /> <img display="inline-block" src="https://github.com/bxxfighting/together-go/blob/master/data/pets/WechatIMG449.jpeg" width="196" hegiht="400" /> <img display="inline-block" src="https://github.com/bxxfighting/together-go/blob/master/data/pets/WechatIMG450.jpeg" width="196" hegiht="400" />
<img display="inline-block" src="https://github.com/bxxfighting/together-go/blob/master/data/pets/WechatIMG448.jpeg" width="196" hegiht="400" /> <img display="inline-block" src="https://github.com/bxxfighting/together-go/blob/master/data/pets/WechatIMG447.jpeg" width="196" hegiht="400" /> <img display="inline-block" src="https://github.com/bxxfighting/together-go/blob/master/data/pets/WechatIMG446.jpeg" width="196" hegiht="400" />
<img display="inline-block" src="https://github.com/bxxfighting/together-go/blob/master/data/pets/WechatIMG445.jpeg" width="196" hegiht="400" /> <img display="inline-block" src="https://github.com/bxxfighting/together-go/blob/master/data/pets/WechatIMG444.jpeg" width="196" hegiht="400" /> <img display="inline-block" src="https://github.com/bxxfighting/together-go/blob/master/data/pets/WechatIMG443.jpeg" width="196" hegiht="400" />
<img display="inline-block" src="https://github.com/bxxfighting/together-go/blob/master/data/pets/WechatIMG442.jpeg" width="196" hegiht="400" /> <img display="inline-block" src="https://github.com/bxxfighting/together-go/blob/master/data/pets/WechatIMG441.jpeg" width="196" hegiht="400" /> <img display="inline-block" src="https://github.com/bxxfighting/together-go/blob/master/data/pets/WechatIMG440.jpeg" width="196" hegiht="400" />
<img display="inline-block" src="https://github.com/bxxfighting/together-go/blob/master/data/pets/WechatIMG439.jpeg" width="196" hegiht="400" /> <img display="inline-block" src="https://github.com/bxxfighting/together-go/blob/master/data/pets/WechatIMG438.jpeg" width="196" hegiht="400" /> <img display="inline-block" src="https://github.com/bxxfighting/together-go/blob/master/data/pets/WechatIMG437.jpeg" width="196" hegiht="400" />
<img display="inline-block" src="https://github.com/bxxfighting/together-go/blob/master/data/pets/WechatIMG436.jpeg" width="196" hegiht="400" /> <img display="inline-block" src="https://github.com/bxxfighting/together-go/blob/master/data/pets/WechatIMG435.jpeg" width="196" hegiht="400" /> <img display="inline-block" src="https://github.com/bxxfighting/together-go/blob/master/data/pets/WechatIMG434.jpeg" width="196" hegiht="400" />
<img display="inline-block" src="https://github.com/bxxfighting/together-go/blob/master/data/pets/WechatIMG433.jpeg" width="196" hegiht="400" />
