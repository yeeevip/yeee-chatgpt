<template>
	<view class="bg" @touchstart="handleTouchStart">
		<image src="" mode="scaleToFill" class="bg-img"></image>
<!-- 		<view class="advertising">
			<ad unit-id="adunit-94ed6bcc5bb80d7f"></ad>
		</view> -->
		<scroll-view scroll-with-animation :scroll-y="isScroll" style="width: 100%;" :scroll-into-view="intoindex">
			<!-- 消息 -->
			<view scroll-with-animation>
				<view class="flex-column-start" v-for="(o,i) in msgList" :key="i" :id="'text'+i">
					<!-- 用户提问-->
					<view v-if="o.my" class="userinfo">
						<view class="flex justify-end my-info">
							<view class="usermsg" :style="[{'max-width': msgMaxWidth + 'rpx'}]">
								<text style="word-break: break-all;" selectable="true" @click="copy(o.msg)">{{o.msg}}</text>
							</view>
							<view class="chat-img" style="margin-right: 12rpx;">
								<image style="height: 80rpx;width: 80rpx;" :src="userAvatar" mode="aspectFit">
								</image>
							</view>
						</view>
					</view>
					<!-- AI回复 -->
					<view v-if="!o.my" class="aiinfo">
						<view class="chat-img">
							<image style="height: 60rpx;width: 60rpx;" src="../../static/1.png" mode="scaleToFill">
							</image>
						</view>
						<view class="flex" :style="[{'max-width': msgMaxWidth + 'rpx'}]">
							<view class="aimsg" style="border-radius: 35rpx;background-color: #f9f9f9;">
								<text style="word-break: break-all;" selectable="true" @click="copy(o.output)">{{o.output}}</text>
							</view>
						</view>
					</view>
				</view>
				<!-- 防止消息底部被遮 -->
				<view style="height: 230rpx;">
				</view>
			</view>
		</scroll-view>
		<!-- 底部导航栏 -->
		<view class="flex-column-center">
			<view class="inpubut">
				<!-- <uni-icons type="settings-filled" color="#fff" size="25" @click="showDrawer" class="ml-10"></uni-icons> -->
				<view class="inputBox" :style="[{bottom: bottomVal}]">
					<textarea v-model="msg" :maxlength="maxlength" class="dh-input" @confirm="sendMsg" confirm-type="search"
						:style="[{height: currentHeight + 'rpx'}]"
						ref="textarea"
						@input=""
						auto-height="true"
						:adjust-position="false"
						:show-confirm-bar="showConfirm"
						placeholder-class="my-neirong-sm" placeholder="描述的问题越详细回答的越准确哦~" 
						@focus="inputBindFocus"
						@blur="inputBindBlur">
					</textarea>
					<image @click="sendMsg" v-if="isRequesting == false" class="imageBtn" src="../../static/send1.png" mode="scaleToFill"/>
					<image v-if="isRequesting == true" class="imageBtn" src="../../static/send2.png" mode="scaleToFill"/>
					<view class="dh-input-counter">{{ msg.length }}/{{ maxlength }}</view>
				</view>
<!-- 				<button @click="sendMsg" :disabled="msgLoad" class="btn" v-if="showSendBtn">
					{{num<=0?'次数超限':isRequesting?'请求中...':sentext}}
				</button> -->
			</view>
		</view>
		<!-- 预设文本 -->
<!-- 		<uni-drawer ref="showDrawer" mode="left" :mask-click="true">
			<view class="drawer-title">
				你想AI扮演什么角色？
			</view>
			<scroll-view style="height: 90%;" scroll-y="true" class="drawer-list">
				<view v-for="(item,index) in promptList" :key="index" @click="setPrompt(item)">
					{{index+1}}.{{item.title}}
				</view>
			</scroll-view>
		</uni-drawer> -->
	</view>
</template>

<script>
import {promptList} from '@/static/js/prompt.js'

const uuid = require('uuid')
export default {
  data() {
    return {
      showConfirm: false,
      maxlength: 200,
      promptList,
      errorMsg: '(￣ε ￣)不好意思呀~~~，服务器开小差了，请稍后重试',
      intoindex: '',
      showShareBtn: false,
      showSendBtn: true,
      rewardedVideoAd: null, //广告
      num: 20, //次数
      isScroll: true, //是否可以滑动
      userAvatar: '', //头像
      msgLoad: false,
      isRequesting: false,
      msgList: [{
        output: '你好，请问有什么可以帮到您的吗？'
      }],
      msgContent: [],
      msg: "",
      ws: null,
      messages: [],
      baseHttpAddr: 'https://www.yeee.vip',
      // baseHttpAddr: 'http://127.0.0.1:8801',
      baseWsAddr: 'wss://www.yeee.vip',
      // baseWsAddr: 'ws://127.0.0.1:8801',
      chatId: null,
      isWx: true,
      minHeight: 100, // 文本输入框最小高度
      currentHeight: 100,// 当前文本输入框高度
      bottomVal: "",
      msgMaxWidth: '550rpx',
      utoken: '',
      isMobile: true,
	  isScrolling: false // 滑动状态
    }
  },
  computed: {
    sentext: function () {
      return `发送(${this.num}次)`
    }
  },
  created() {

  },
  mounted() {
    // console.log(this.$el);
    this.msgMaxWidth = Math.max(this.getMaxWidth(), 550)
  },
  onShow() {
    const vm = this
    // vm.sseInit()
    vm.wsInint2()
  },
  destroyed() {
    const vm = this
    if (vm.ws) {
      vm.ws.close()
    }
  },
  onHide() {
    const vm = this
    if (vm.ws) {
      vm.ws.close()
    }
    vm.isRequesting = false;
    vm.msgLoad = false
  },
  onLoad() {
    var vm = this;
    if (this.isWechat()) {
      // 激励广告
      if (wx.createRewardedVideoAd) {
        vm.rewardedVideoAd = wx.createRewardedVideoAd({
          adUnitId: 'adunit-3b44d1532c13cf38'
        })
        vm.rewardedVideoAd.onLoad(() => {
          console.log('onLoad event emit')
        })
        vm.rewardedVideoAd.onError((err) => {
          console.log('onError event emit', err)
          vm.toSendMsg()
        })
        vm.rewardedVideoAd.onClose((res) => {
          console.log('onClose event emit', res)
          if (res.isEnded) {
            uni.request({
              url: vm.baseHttpAddr + '/api/airobot/ad/onClose/' + vm.utoken,
              method: 'POST',
              success: (res) => {
                console.log('ad/onClose emit', res)
                if (res.data.code == 200) {
                  vm.num += res.data.data.incr
                } else {

                }
              }
            })
          } else {
            vm.toSendMsg()
          }
        })
      }
      wx.getSystemInfo({
        success: function(res) {
            // 判断设备类型
          if (res.platform === "windows" || res.platform === "mac") {
            // PC端
            vm.isMobile = false;
          } else {
            // 手机端
          }
        }
      })
      // wx.showShareMenu({
      // 	withShareTicket: true,
      // 	//设置下方的Menus菜单，才能够让发送给朋友与分享到朋友圈两个按钮可以点击
      // 	menus: ["shareAppMessage", "shareTimeline"]
      // })
    }

    const that = this;
    // this.userAvatar = uni.getStorageSync('user-avatar')
    this.userAvatar = '../../static/2.png'
  },
  methods: {
    wsInint2() {
      const vm = this
      if (vm.chatId || !vm.isWechat()) {
        if (!vm.isWechat() && !vm.chatId) {
          vm.chatId = vm.getUrlParams(window.location.href)['chatId'] ? vm.getUrlParams(window.location.href)['chatId'] : uuid.v1()
        }

        uni.request({
          url: vm.baseHttpAddr + '/api/airobot/ws-auth',
          method: 'GET',
          success: (res) => {
            if (res.data.code == 200) {
              vm.utoken = res.data.data.token
              vm.wsInit(res.data.data.token)
              vm.num = res.data.data.limitCount
              if (res.data.data.limitCount) {
                if (vm.num === 1000) {
                  vm.maxlength = 2000
                }
              }
            } else {
              vm.msgList[vm.msgList.length - 1].output = '认证失败'
              vm.isRequesting = false;
              vm.msgLoad = false
            }
          },
          fail: (err) => {
            vm.msgList[vm.msgList.length - 1].output = '认证失败'
            vm.isRequesting = false;
            vm.msgLoad = false
          }
        })
      } else {
        uni.login({
          provider: 'weixin',
          success: function (loginRes) {
            uni.request({
              url: vm.baseHttpAddr + '/api/airobot/ws-auth?jscode=' + loginRes.code,
              method: 'GET',
              success: (res) => {
                if (res.data.code == 200) {
                  vm.chatId = res.data.data.openid ? res.data.data.openid : uuid.v1()
                  vm.utoken = res.data.data.token
                  vm.wsInit(res.data.data.token)
                  vm.num = res.data.data.limitCount
                  if (res.data.data.limitCount) {
                    if (vm.num === 1000) {
                      vm.maxlength = 2000
                    }
                  }
                } else {
                  vm.msgList[vm.msgList.length - 1].output = '认证失败'
                  vm.isRequesting = false;
                  vm.msgLoad = false
                }
              },
              fail: (err) => {
                vm.msgList[vm.msgList.length - 1].output = '认证失败'
                vm.isRequesting = false;
                vm.msgLoad = false
              }
            })
          }
        });
      }
    },
    wsInit(t) {
      const vm = this
      if (vm.ws) {
        vm.ws.close()
      }
      vm.ws = uni.connectSocket({
        url: vm.baseWsAddr + '/ws/airobot/' + vm.chatId + '/' + t,
        complete: () => {
        }
      })
      vm.ws.onMessage(function (e) {
        let msgRecv = JSON.parse(e.data)
        let found = false
        if (msgRecv.kind == 'heart') {
          console.log("heart~~~~")
          return
        }
        vm.msgList.forEach((m, i) => {
          if (m.msgId === msgRecv.msgId) {
            found = true
            if (m.kind !== 'chat') {
              vm.msgList[i].output = ''
              m.kind = 'chat'
              vm.num--
            }
            vm.isRequesting = false
            vm.msgList[i].output = vm.msgList[i].output + msgRecv.msg
			if (!vm.isScrolling) {
				vm.$nextTick(() => {
				  // vm.intoindex = "text" + (vm.msgList.length - 1)
				  uni.pageScrollTo({
				    scrollTop: 2000000,    //滚动到页面的目标位置（单位px）
				    duration: 0    //滚动动画的时长，默认300ms，单位 ms
				  });
				});
			}
          }
        })
        if (!found) {
          let text = msgRecv.msg
          let msg = {
            msgId: msgRecv.msgId,
            output: text,
            kind: msgRecv.kind
          }
          vm.msgList.push(msg)
          if (msgRecv.kind === 'chat') {
            vm.isRequesting = false;
          }
          vm.msgContent.push({
            "role": '',
            "content": text,
          })
          vm.msgLoad = false
          vm.$nextTick(() => {
            // vm.intoindex = "text" + (vm.msgList.length - 1)
			if (!this.isScrolling) {
				uni.pageScrollTo({
				  scrollTop: 2000000,    //滚动到页面的目标位置（单位px）
				  duration: 0    //滚动动画的时长，默认300ms，单位 ms
				});
			}
          });
        }

      })
      vm.ws.onOpen(function (e) {
        console.log('ws open')
      })
      vm.ws.onClose(function (e) {
        console.log('ws close')
      })
      vm.ws.onError(function () {
        vm.msgList[vm.msgList.length - 1].output = vm.errorMsg
        vm.isRequesting = false;
        vm.msgLoad = false
      })
    },
    reconnectWs() {
      const vm = this
      vm.ws.closeSocket()
      vm.ws = null
      vm.wsInit()
    },
    setPrompt(item) {
      this.msg = item.prompt;
      this.sendMsg();
      this.$refs.showDrawer.close();
    },
    showDrawer() {
      this.$refs.showDrawer.open();
    },
    /* 			shareFriends() {
            const vm = this
            uni.share({
              provider: 'weixin',
              scene: 'WXSenceTimeline',
              title: '微搜小助手',
              success: (res) => {

              },
              fail: (err) => {

              }
            })
            vm.num += 5;
            vm.showShareBtn = false;
            vm.showSendBtn = true
          }, */
    sendMsg() {
      const vm = this;
      uni.request({
        url: vm.baseHttpAddr + '/api/airobot/chat/surplus/' + vm.utoken,
        method: 'POST',
        success: (res) => {
          if (res.data.code == 200) {
            if (res.data.data.limitCount > 0) {
              this.toSendMsg()
              return
            } else {
              this.remindWatchAdGainCount()
              return
            }
          }
          this.toSendMsg()
        }
      })
    },
    toSendMsg() {
      const vm = this;
      const that = this;
	  vm.isScrolling = false
      // 消息为空不做任何操作
      if (this.msg == "") {
        return;
      }
      if (this.isRequesting) {
        return;
      }
      uni.pageScrollTo({
        scrollTop: 2000000,    //滚动到页面的目标位置（单位px）
        duration: 0    //滚动动画的时长，默认300ms，单位 ms
      });
      this.isRequesting = true;
      this.msgList.push({
        "msg": this.msg,
        "my": true
      })
      this.msgContent.push({
        "role": "user",
        "content": this.msg,
      })
      this.msgLoad = true
      vm.ws.send({
        data: this.msg
      })
      // 清除消息
      this.msg = ""
      vm.currentHeight = 100
    },
    copy(text) {
      if (!this.isWx) {
        return
      }
      //提示模板
      uni.showModal({
        content: text,//模板中提示的内容
        confirmText: '复制内容',
        success: (res) => {//点击复制内容的后调函数
          if (res.confirm) {
            //uni.setClipboardData方法就是讲内容复制到粘贴板
            uni.setClipboardData({
              data: text,//要被复制的内容
              success: () => {//复制成功的回调函数
                uni.showToast({//提示
                  title: '复制成功'
                })
              }
            });
          } else if (res.cancel) {

          }
        }
      });
    },
    isWechat() {
      return this.isWx
      // return this.isMiniProgram()
    },
    getMaxWidth() {
      var aa = uni.getSystemInfoSync().windowWidth;
      return (aa - 100)
    },
    getUrlParams(url) {
      let obj = {};
      // 通过 ? 分割获取后面的参数字符串
      let urlStr = url.split('?')[1]
      if (!urlStr) {
        return obj
      }
      // 创建空对象存储参数
      // 再通过 & 将每一个参数单独分割出来
      let paramsArr = urlStr.split('&')
      for (let i = 0, len = paramsArr.length; i < len; i++) {
        // 再通过 = 将每一个参数分割为 key:value 的形式
        let arr = paramsArr[i].split('=')
        obj[arr[0]] = arr[1];
      }
      return obj
    },
    onInput(e) {
      const vm = this
      vm.msg = e.target.value;
      vm.$nextTick(() => {
        const height = vm.$refs.textarea.$el.scrollHeight; // 注意此处获取高度的方式
        // 若高度变化，则更新文本输入框高度
        if (height > vm.minHeight && height !== vm.currentHeight) {
          vm.currentHeight = height;
        }
      });
    },
    inputBindFocus(e) {
      // isScroll=false;
      this.bottomVal = e.detail.height + 'px'
	  this.isScrolling = false
    },
    inputBindBlur() {
      // isScroll=true;
      this.bottomVal = ''
    },
    remindWatchAdGainCount() {
      var vm = this;
      uni.showModal({
        content: '你的免费次数不足，观看广告即可获得免费次数！反馈建议QQ:1324459373',
        confirmText: '去观看',
        success: (res) => {
          if (res.confirm && vm.isMobile) {
            vm.rewardedVideoAd.show()
          } else {
            vm.toSendMsg()
          }
        }
      });
    },
	handleTouchStart () {
		this.isScrolling = true
	}
  }
}
</script>

<style>
	page {
		height: 100%;
	}
	.drawer-list{
		padding:0 20rpx;
	}
	.advertising {}
	.drawer-title{
		text-align: center;
		padding: 20rpx;
		color:  #616161;
	}
	.drawer-list view{
		margin-top: 20rpx;
	}
	.ml-10{
		margin-left: 10rpx;
	}
	.bg {
		/* overflow: scroll; */
		/* background: url('../../static/6.png') no-repeat;
		background-size: 100% 100%; */
		margin-top: 30rpx;
		width: 100%;
		height: 100%;
	}

	.bg-img {
		width: 100%;
		height: 100%;
		position: fixed;
	}

	.userinfo {
		animation: oneshow 0.8s ease 1;
		display: flex;
		flex-direction: column;
		align-items: flex-end;
		/* padding-right: 20rpx; */
		margin-top: 20rpx;
	}

	.usermsg {
		margin-right: 0rpx;
		padding: 17rpx 20rpx;
		border-radius: 35rpx;
		background-color: #f9f9f9 !important;
		// margin-top: 20rpx;
	}

	.my-info {
		display: flex;
		align-items: center;
		animation: oneshow 0.8s ease 1;
		margin-left: 65rpx;
	}

	.aiinfo {
		display: flex;
		flex-direction: row;
		align-items: center;
		margin-left: 20rpx;
		margin-top: 20rpx;
		animation: oneshow 0.8s ease 1;
	}

	.chat-img {
		overflow: hidden;
		border-radius: 30%;
		/* width: 100rpx;
		height: 100rpx; */
		/* background-color: #f7f7f7;file:///D:/360安全浏览器下载/2 (3).png */
		display: flex;
		flex-direction: row;
		justify-content: center;
		align-items: center;
	}

	.aimsg {
		display: flex;
		flex-direction: column;
		justify-content: center;
		margin-left: 20rpx;
		margin-right: -40rpx;
		padding: 17rpx 20rpx;
	}

	.flex-column-center {
		display: flex;
		flex-direction: column;
		justify-content: center;
		align-items: center;
		position: fixed;
		bottom: 0px;
		width: 100%;
	}

	.inpubut {
		display: flex;
		flex-direction: row;
		justify-content: space-around;
		align-items: center;
		/* background-color: #f9f9f9; */
		width: 100%;
		/* height: 110rpx; */
		font-size: 40rpx;
	}
	.inputBox {
		display: flex;
		flex-direction: row;
		align-items: center;
		width: 100%;
		position: relative;
	}
	.dh-input {
		flex: 1;
		width: 100%;
		min-height: 100rpx;
		height: 100rpx;
		border-radius: 50rpx;
		padding-top: 20rpx;
		padding-left: 40rpx;
		padding-right: 100rpx;
		padding-bottom: 40rpx;
		margin-left: 20rpx;
		margin-right: 20rpx;
		margin-bottom: 45rpx;
		resize: none;
		/* overflow: hidden; // 隐藏超出部分 */
		background-color: #f0f0f0;
		font-size: 32rpx;
		cursor-spacing: 300rpx
	}
	.my-neirong-sm {
		font-size: 32rpx;
		color: #616161;
	}
	.btn {
		height: 100rpx;
		line-height: 100rpx;
		white-space: nowrap;
		background: linear-gradient(to right, #008FFF, #29C8FC);
		color: #ffffff;
		font-size: 32rpx;
		border-radius: 2500rpx;
		margin: 0 20rpx;
		margin-bottom: 45rpx;
	}
	.popcls {
		width: 80vw;
		height: 40vh;
		background: white;
		border-radius: 20rpx;
		display: flex;
		flex-direction: column;
		justify-items: center;
		align-items: center;
	}
  .dh-input-counter {
    font-size: 20rpx;
    color: #999;
    margin-top: 5rpx;
    text-align: left;
    position: absolute;
    left: 50rpx;
    bottom: 50rpx;
  }
  .imageBtn {
  	position: absolute;
  	height: 100rpx;
  	width: 100rpx;
    text-align: right;
    right: 40rpx;
    bottom: 70rpx;
  }
</style>