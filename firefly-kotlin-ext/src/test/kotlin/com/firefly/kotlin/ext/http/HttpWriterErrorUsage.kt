package com.firefly.kotlin.ext.http

import com.firefly.codec.http2.model.HttpHeader
import com.firefly.codec.http2.model.HttpMethod
import com.firefly.codec.http2.model.MimeTypes
import com.firefly.kotlin.ext.annotation.NoArg
import com.firefly.kotlin.ext.common.Json
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import java.io.Serializable

/**
 * @author Pengtao Qiu
 */
@NoArg
data class AppColumnVO(
    var id: Long,
    var name: String,
    var englishName: String?,
    var order: Int,
    var cover: String,
    var url: String,
    var token: String?,
    var duration: Long?,
    var backgroundImage: String?,
    var episodeNum: Int?
                      ) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1
    }

    constructor() : this(0, "", "", 0, "", "", "", 0, "", 0)
}

@NoArg
data class Response<T>(var code: Int, var msg: String?, var data: T) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1
    }
}

val jsonString = """
{
  "code": 200,
  "msg": null,
  "data": [
    {
      "id": 28,
      "name": "行为动作",
      "englishName": "Hello Hello",
      "order": 1,
      "cover": "http://img.gsxservice.com/48620577_f7mbw2vd.png",
      "url": "",
      "token": "MTg1X2FlZjdjYjJiNTZkMjJkOWQ2Y2EyZGQxNTNiYmIyZDU4",
      "duration": 91,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 168,
      "name": "日常生活",
      "englishName": "The bath song",
      "order": 2,
      "cover": "http://img.gsxservice.com/48623663_x8fe8zss.png",
      "url": "",
      "token": "MjAxXzVmN2E3N2Q5MWRiYjQxMTllZTFkMGQxYWY3NDM0OTE4",
      "duration": 136,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 27,
      "name": "简单入门",
      "englishName": "Hello song",
      "order": 3,
      "cover": "http://img.gsxservice.com/48620461_a0g19chp.png",
      "url": "",
      "token": "MTg0X2Y3NTU1OWUzNjg4YzYzZDIwN2Q1ZWRiY2U4ZmYxYjE4",
      "duration": 84,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 458,
      "name": "大动作游戏",
      "englishName": "We all fall down",
      "order": 4,
      "cover": "http://img.gsxservice.com/52215368_ok7hvy2d.png",
      "url": "",
      "token": "NTYwXzJlMzA4ZmVlMWE3MjMyZjhiYWFiN2M1NzIwNjNlMjY0",
      "duration": 129,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 763,
      "name": "数字认知",
      "englishName": "Once I caught a fish alive",
      "order": 5,
      "cover": "http://img.gsxservice.com/56521360_591zpuf8.png",
      "url": "",
      "token": "MTAxMl85ZTA1ZDgwYTI2ZjFhMzZkN2MyNzAxNzNjM2ZlNWVjNQ==",
      "duration": 76,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 12,
      "name": "数字",
      "englishName": "Say cheese",
      "order": 6,
      "cover": "http://img.gsxservice.com/48612825_zyoges1d.png",
      "url": "",
      "token": "MTY5XzBhNzA4OWE3NWEwMmRiYzdjYzBmMjMyNGUzMWQwNWQ5",
      "duration": 117,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 13,
      "name": "鹅妈妈童谣",
      "englishName": "One two buckle my shoe",
      "order": 7,
      "cover": "http://img.gsxservice.com/48612896_wxhxiq3g.jpeg",
      "url": "",
      "token": "MTcwXzE3ODY4N2IyNGRiNDQwMTdkODNkZTkyYTRiNzNlNTAz",
      "duration": 41,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 14,
      "name": "儿歌经典",
      "englishName": "One little finger",
      "order": 8,
      "cover": "http://img.gsxservice.com/48612950_utx6cf61.jpeg",
      "url": "",
      "token": "MTcxXzZkNzg1OTRkMWRjMWIyYWNiYmJjMDg3NTM4MmM2YzQx",
      "duration": 144,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 15,
      "name": "圣诞金曲",
      "englishName": "Jingle bell",
      "order": 9,
      "cover": "http://img.gsxservice.com/48613042_bosyb3vb.png",
      "url": "",
      "token": "MTcyX2E3YzkxODE5OWFjNDg0ZTQ4NjE1ZDhkMmM2MDExM2Y2",
      "duration": 189,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 16,
      "name": "动物认知",
      "englishName": "Let's go to the zoo",
      "order": 10,
      "cover": "http://img.gsxservice.com/48613099_ex3437h1.jpeg",
      "url": "",
      "token": "MTczXzk5YTA2NTcyMzJjNDUyNzBkYzg5MzQ2YWQ5NWJkYmFi",
      "duration": 239,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 17,
      "name": "儿歌经典",
      "englishName": "London bridge is falling down",
      "order": 11,
      "cover": "http://img.gsxservice.com/48613187_v9damig5.png",
      "url": "",
      "token": "MTc0XzYzMWU0YmMzNWZkMDM4NzAwMjAyMWI2ODRhNzc2YzM0",
      "duration": 113,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 18,
      "name": "儿歌经典",
      "englishName": "Mary had little lamb",
      "order": 12,
      "cover": "http://img.gsxservice.com/48613223_rwnav6us.jpeg",
      "url": "",
      "token": "MTc1XzllZGE5NDE1MzdjYmU4ZjA3MWE5OWI3YTMzMzRjOWE0",
      "duration": 129,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 19,
      "name": "郎朗上口",
      "englishName": "Humpty",
      "order": 13,
      "cover": "http://img.gsxservice.com/48613689_cqkh4xut.png",
      "url": "",
      "token": "MTc2Xzg3MDgzYjgyMDMyNDI2MTA1NmUyOWIzZmM4OTRhNzA2",
      "duration": 139,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 20,
      "name": "颜色认知",
      "englishName": "I can sing a rainbow",
      "order": 14,
      "cover": "http://img.gsxservice.com/48613784_vzt0wj71.png",
      "url": "",
      "token": "MTc3X2ZmOTA1YTRlMDllOTY4MTBjMTAzOGRkN2Q0ZDlkNmQ2",
      "duration": 75,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 21,
      "name": "动物认知",
      "englishName": "I have a pet",
      "order": 15,
      "cover": "http://img.gsxservice.com/48615177_ulm1uzm5.jpeg",
      "url": "",
      "token": "NDBfZGExMGYxYzkyMDg3MGU3YzdlMGZkM2M4NzdiYzU2NWQ=",
      "duration": 144,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 22,
      "name": "乐器认知",
      "englishName": "I'm a music man",
      "order": 16,
      "cover": "http://img.gsxservice.com/48615298_w7tflyu0.jpeg",
      "url": "",
      "token": "MTc5X2VhMDU0YTIwOTFjOWZlYmU0ODlhMTQyYTI3ZjMyMjcz",
      "duration": 353,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 23,
      "name": "情感认知",
      "englishName": "If you are happy",
      "order": 17,
      "cover": "http://img.gsxservice.com/48615489_hiidqvhv.png",
      "url": "",
      "token": "MTgwX2M5ZDU1NDJmYTMwYzc3Yjg4NDc1MDY3NTlkZTQwYjFi",
      "duration": 123,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 24,
      "name": "数字认知",
      "englishName": "Five little ducks",
      "order": 18,
      "cover": "http://img.gsxservice.com/48615518_c75759cq.jpeg",
      "url": "",
      "token": "MTgxX2QyZmVjOTdmZWJhOTEwZTA1ZmE4OTZlOWNlNzA5Njli",
      "duration": 117,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 25,
      "name": "情感认知",
      "englishName": "Go away scary monster",
      "order": 19,
      "cover": "http://img.gsxservice.com/48615613_ty03kn23.png",
      "url": "",
      "token": "MTgyXzMyZDIyNWRmYjU4MDJjODE5MWYwMjgwMTRmZWUwNDcx",
      "duration": 114,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 26,
      "name": "身体部位",
      "englishName": "Head shoulder knees and toes",
      "order": 20,
      "cover": "http://img.gsxservice.com/48620263_cwn5h5v6.png",
      "url": "",
      "token": "MTgzXzE4MjMwMDgyNjkwMWQ1NmNhMjZhNzlkZDMwOTBlYjcz",
      "duration": 60,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 29,
      "name": "朗朗上口",
      "englishName": "Hey diddle diddle",
      "order": 21,
      "cover": "http://img.gsxservice.com/48620995_zn3096nr.png",
      "url": "",
      "token": "MTg2X2Q2YzY2NDNmZTE1NmZiOTVhNmI4ZDc3N2M0YzM5ZDVk",
      "duration": 39,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 30,
      "name": "有趣",
      "englishName": "Hickory dickory dock",
      "order": 22,
      "cover": "http://img.gsxservice.com/48621161_wpsvyy9s.png",
      "url": "",
      "token": "MTg3XzZhOTA1ZDc3OTc1ZTI1OGZlYWZmMDNhMDYzODJlN2Yy",
      "duration": 181,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 31,
      "name": "游戏",
      "englishName": "Hide and seek",
      "order": 23,
      "cover": "http://img.gsxservice.com/48621368_3bdi4t4e.png",
      "url": "",
      "token": "MTg4X2IzOGUwNWEwYzU5YmY4NWE5ZDIwODEzMjJlNTZlOTNm",
      "duration": 193,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 32,
      "name": "天气认知",
      "englishName": "How's the weather",
      "order": 24,
      "cover": "http://img.gsxservice.com/48621430_9mir99ir.png",
      "url": "",
      "token": "MTg5Xzk4NjdjYTQ2ZTMyMmNkMzk5MGY2MGNjZDdlYTExZDM4",
      "duration": 126,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 33,
      "name": "食物认知",
      "englishName": "Do you like brocooli",
      "order": 25,
      "cover": "http://img.gsxservice.com/48621653_fdb7j8rm.jpeg",
      "url": "",
      "token": "MTkwX2VjOTBhNjFiYzVkMTJmM2FjY2IyZTRmNjI0ZWEwOTQz",
      "duration": 162,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 34,
      "name": "简单入门",
      "englishName": "Bingo",
      "order": 26,
      "cover": "http://img.gsxservice.com/48621840_pnwu1j0m.png",
      "url": "",
      "token": "MTkxX2RhNWQ2YjY4YjhmMjFlMTczMWUzMWY5MTIyYjQ4NjVj",
      "duration": 348,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 35,
      "name": "习惯养成",
      "englishName": "Clean up",
      "order": 27,
      "cover": "http://img.gsxservice.com/48622024_wcd15l42.png",
      "url": "",
      "token": "MTkyX2MyOGQ0MTc2YzM0YjQ0NjIyNmYwYmY5NWM1ZTZjMTBl",
      "duration": 109,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 40,
      "name": "情感认知",
      "englishName": "Can you make a happy face",
      "order": 28,
      "cover": "http://img.gsxservice.com/48622307_p3odf2wn.jpeg",
      "url": "",
      "token": "MTkzXzVjZTRiOGVjZjgzODYzMGJiMDg1MWZlMjdlOGMzYjY5",
      "duration": 120,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 41,
      "name": "儿歌经典",
      "englishName": "Baby bumblebee",
      "order": 29,
      "cover": "http://img.gsxservice.com/48622489_7a8h6djk.png",
      "url": "",
      "token": "NDlfYWM0NjNkYTkzYWY3N2UxZDk3ZThkYjY0ODE0YjU3MTc=",
      "duration": 76,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 163,
      "name": "英美经典",
      "englishName": "Jack and Jill",
      "order": 30,
      "cover": "http://img.gsxservice.com/48622785_bwl3sudn.png",
      "url": "",
      "token": "MTk2X2RmMTRjM2ZjMWI0NTFmNTU2YWUyYWU3NzQ5NTBiNjM4",
      "duration": 65,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 164,
      "name": "简单入门",
      "englishName": "Apple tree",
      "order": 31,
      "cover": "http://img.gsxservice.com/48622888_2hicur48.jpeg",
      "url": "",
      "token": "MTk3XzE3N2ViOGRjYWM0YTk3ODNhYzNhNzdkZDAzY2QxN2Ji",
      "duration": 71,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 165,
      "name": "简单入门",
      "englishName": "Peekaboo",
      "order": 32,
      "cover": "http://img.gsxservice.com/48623038_m8uxe99b.png",
      "url": "",
      "token": "MTk4X2JiOWEzZDJjMTYxNjBjODZkMzlkMzhiZmIxYTA0OTJk",
      "duration": 110,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 166,
      "name": "经典童谣",
      "englishName": "Wash the dishes",
      "order": 33,
      "cover": "http://img.gsxservice.com/48623440_s7u1hgp7.png",
      "url": "",
      "token": "MTk5X2FhMDhmMDljMWVkMDQ5YmIwNjU2OGJhYzA2MTFhMTM0",
      "duration": 148,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 167,
      "name": "日常生活",
      "englishName": "Put on your shoes",
      "order": 34,
      "cover": "http://img.gsxservice.com/48623511_y7mc8cni.png",
      "url": "",
      "token": "MjAwXzAyMmQ2NTE2OTExNjY1OWM3YjJmMWNlOWNjNzMxOTY4",
      "duration": 181,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 169,
      "name": "时间观念",
      "englishName": "The months chat",
      "order": 35,
      "cover": "http://img.gsxservice.com/48623997_rdpdochf.jpeg",
      "url": "",
      "token": "MjAyXzRhYmFlZGZiYWJkNTQ5NGVhMGMyNjNhMjFhZTA3OTc2",
      "duration": 138,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 170,
      "name": "永恒经典",
      "englishName": "You are my sunshine",
      "order": 36,
      "cover": "http://img.gsxservice.com/48624092_jqgxvzwt.png",
      "url": "",
      "token": "MjAzXzQyYjBmNjMyYTY4ZTRhYmZkNWQ5ZGUyY2NkMzk4NzQw",
      "duration": 80,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 171,
      "name": "游戏动作",
      "englishName": "The bunny hop",
      "order": 37,
      "cover": "http://img.gsxservice.com/48625356_nghyf3uz.png",
      "url": "",
      "token": "MjA0XzJjYzdjZWNmZDNkZmU1Y2ZjM2VlOGZmZTFkMDgyNzYw",
      "duration": 121,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 172,
      "name": "游戏歌谣",
      "englishName": "Rock scissors paper",
      "order": 38,
      "cover": "http://img.gsxservice.com/48625554_l564sq81.jpeg",
      "url": "",
      "token": "MjA1X2VhOGMwZjI1N2MwM2RmM2YzZDRhMjdiYTk5M2VlNjc2",
      "duration": 109,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 173,
      "name": "对比词",
      "englishName": "Open shut them",
      "order": 39,
      "cover": "http://img.gsxservice.com/48625777_vh0q7u1u.png",
      "url": "",
      "token": "MjA2XzkzNmU3OWY0YWI4YWE4NDA1MjNiNTQ5MzUzMDkxMmIz",
      "duration": 147,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 174,
      "name": "日常生活",
      "englishName": "Please sit down",
      "order": 40,
      "cover": "http://img.gsxservice.com/48626026_yc5jtn8d.jpeg",
      "url": "",
      "token": "MjA3XzkzNjc1ZDNmZmRjMjE2NDQ0YTU5ZTZmN2ZiMDM3YTYy",
      "duration": 62,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 175,
      "name": "数字主题",
      "englishName": "Seven steps",
      "order": 41,
      "cover": "http://img.gsxservice.com/48626105_1ym5x0w8.png",
      "url": "",
      "token": "MjA4Xzc2ZjlkYzJhMDViN2QwNTIwYWFiZTJmMTA2MTc5OTVi",
      "duration": 88,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 176,
      "name": "数字主题",
      "englishName": "Ten little dinosaurs",
      "order": 42,
      "cover": "http://img.gsxservice.com/48626194_cta6vyvk.png",
      "url": "",
      "token": "MjA5XzYzZGFmNDI0ZDBhNjU4MmRkOTI1MWNmMzQwMGMxZGVm",
      "duration": 114,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 177,
      "name": "数字主题",
      "englishName": "Two little black birds",
      "order": 43,
      "cover": "http://img.gsxservice.com/48626376_qwbo8gwo.png",
      "url": "",
      "token": "MjEwXzU2ZTkyMzE2MDk1MjZlZjVkMDUwZmVhYmNkNDIwMjNh",
      "duration": 43,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 178,
      "name": "形状认知",
      "englishName": "The shape song",
      "order": 44,
      "cover": "http://img.gsxservice.com/48626500_iygupl9j.png",
      "url": "",
      "token": "MjExXzhlZTNjNjhmNWI2Nzk5ZmNmMWJkZjVkZDc4N2EwOTI1",
      "duration": 250,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 179,
      "name": "家庭成员",
      "englishName": "Rain rain go away",
      "order": 45,
      "cover": "http://img.gsxservice.com/48626771_laibtwjo.png",
      "url": "",
      "token": "MjEyXzg5ZjI1ZDcyZjYyNWMwZGM2MTJiNmNiMGEwNGVmZjYw",
      "duration": 155,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 180,
      "name": "哄睡首选",
      "englishName": "Row row row your boat",
      "order": 46,
      "cover": "http://img.gsxservice.com/48626820_op4qkd3w.png",
      "url": "",
      "token": "MjEzX2YxZmNhMGJlZmZjYzEyYTM2YjgwNDA5ODFiM2VmN2Nh",
      "duration": 121,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 424,
      "name": "永恒经典",
      "englishName": "The wheels on the bus",
      "order": 47,
      "cover": "http://img.gsxservice.com/52135100_7jhss6hv.png",
      "url": "",
      "token": "NTE5XzAwNTMwMDY4YWIzNmNmOTBmNTk3ZjgzZWJlMWRiYjBl",
      "duration": 147,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 425,
      "name": "日常生活",
      "englishName": "Put on your shoes",
      "order": 48,
      "cover": "http://img.gsxservice.com/52137690_85mebron.png",
      "url": "",
      "token": "NTIxXzY2MzQzMmY5ZDE2NzNhZWJmZGQ5NGRhOGY3OWViODg2",
      "duration": 181,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 428,
      "name": "经典童谣",
      "englishName": "Wash the dishes",
      "order": 49,
      "cover": "http://img.gsxservice.com/52138561_2jx7kzpt.png",
      "url": "",
      "token": "NTIzX2FhNmVjZGYyOWFiNGQ5ZjMyODRlZTA3M2NmOTc2YjFj",
      "duration": 148,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 430,
      "name": "哄睡首选",
      "englishName": "Twinkle twinkle",
      "order": 50,
      "cover": "http://img.gsxservice.com/52139124_smc1zjxh.png",
      "url": "",
      "token": "NTI0X2UzZjBiZGE3YmEwN2RhMDZlNWQxMTZiZGJmY2M3NTk3",
      "duration": 153,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 432,
      "name": "哄睡首选",
      "englishName": "Sweet dreams",
      "order": 51,
      "cover": "http://img.gsxservice.com/52140784_dsvko3pj.png",
      "url": "",
      "token": "NTI2XzEwMTQ2NWRmZjFlY2VlMGI0MWJkMWY0YTMyMWZiNzU1",
      "duration": 187,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 434,
      "name": "儿歌经典",
      "englishName": "To market to market",
      "order": 52,
      "cover": "http://img.gsxservice.com/52141477_o84k48ss.png",
      "url": "",
      "token": "NTI5XzA0OTU2NjE2NGNiYmE1NGU2NDAyMDJlNTg1Nzc2ZjMw",
      "duration": 52,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 436,
      "name": "儿歌经典",
      "englishName": "This old man",
      "order": 53,
      "cover": "http://img.gsxservice.com/52141891_cbo1bblm.png",
      "url": "",
      "token": "NTMxXzg3NDZjMzBhMDkyZDg0MjNkOTc1ZmQwMzE5YWI2N2U1",
      "duration": 156,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 438,
      "name": "儿歌经典",
      "englishName": "The muffin man",
      "order": 54,
      "cover": "http://img.gsxservice.com/52142850_dsx3v30o.png",
      "url": "",
      "token": "NTMzXzZlZjEzYTBlZmQ1MTIyMmE3NTc2OTQzMmNlNDVmMDdj",
      "duration": 88,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 440,
      "name": "儿歌经典",
      "englishName": "The itsy bisty spider",
      "order": 55,
      "cover": "http://img.gsxservice.com/52143778_k5bfvlar.png",
      "url": "",
      "token": "NTM1X2I5OWRmMDE4NGI2MDI2NWY1MjNhYTlkMTAyNjJmYjYz",
      "duration": 92,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 444,
      "name": "儿歌经典",
      "englishName": "London bridge",
      "order": 56,
      "cover": "http://img.gsxservice.com/52145006_ghpj3to9.png",
      "url": "",
      "token": "NTM5X2FiNGE2OGEzOTViNTNhYmQ4YzY1MTRjNmVjOWFjNWE3",
      "duration": 62,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 446,
      "name": "儿歌经典",
      "englishName": "Baba black sheep",
      "order": 57,
      "cover": "http://img.gsxservice.com/52145731_g3o2mzcy.png",
      "url": "",
      "token": "NTQyX2U2MTkwODJmYTcwN2Y3ODZkNDU1ZDk4ZDZkZmU0NTQx",
      "duration": 136,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 448,
      "name": "动物主题",
      "englishName": "Yes, I can",
      "order": 58,
      "cover": "http://img.gsxservice.com/52146265_e52y3bje.png",
      "url": "",
      "token": "NTQ0XzhlMWM0ZmY2ZTlhN2YwODlmMGNjMmY5YjEzNmNjMWYz",
      "duration": 216,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 450,
      "name": "动物主题",
      "englishName": "Who took the cookies",
      "order": 59,
      "cover": "http://img.gsxservice.com/52147366_tcq50m0a.png",
      "url": "",
      "token": "NTQ2X2E4OTUzZGEwN2ExMGM2MmU5YzA3MmVhN2Q0MDc4NDA3",
      "duration": 122,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 452,
      "name": "动物主题",
      "englishName": "Walking in the jungle",
      "order": 60,
      "cover": "http://img.gsxservice.com/52153416_srmlifim.png",
      "url": "",
      "token": "NTQ4XzFhZGJhOWQxODk5OTQwMjY5NzdiYTJkNjE2OTRjZDc2",
      "duration": 204,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 454,
      "name": "动物主题",
      "englishName": "Walking through the jungle",
      "order": 61,
      "cover": "http://img.gsxservice.com/52153530_sp76ua9u.png",
      "url": "",
      "token": "NTUyXzBlOWNhZGQ1ODQ4MWU2NjA3NzZlZjVmZjYwY2FiNmI3",
      "duration": 139,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 456,
      "name": "动物主题",
      "englishName": "Did you ever see my tail",
      "order": 62,
      "cover": "http://img.gsxservice.com/52213372_xfvn8wzg.png",
      "url": "",
      "token": "NTU3XzYxYzI1ZDEzMGNiM2Q3ZmViYTE4YTEzMmRiOTIwMmU5",
      "duration": 80,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 461,
      "name": "大动作游戏",
      "englishName": "Walking walking",
      "order": 63,
      "cover": "http://img.gsxservice.com/52216187_tpkbjfmb.png",
      "url": "",
      "token": "NTYyXzBkOTQxYjhjOTIzYWRhMDEwNzcwNWY5NDgzYjYzMzA0",
      "duration": 99,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 464,
      "name": "大动作游戏",
      "englishName": "The hockey pockey shake",
      "order": 64,
      "cover": "http://img.gsxservice.com/52216939_89fcjbeg.png",
      "url": "",
      "token": "NTY2XzYwNGU2ZjJkYTAzZTkzZDU4NTk5NDRmMmI5MzRjYTAy",
      "duration": 163,
      "backgroundImage": "",
      "episodeNum": 0
    },
    {
      "id": 1233,
      "name": "测试dq",
      "englishName": "hello",
      "order": 333,
      "cover": "http://img.gsxservice.com/68113566_hbb8lh23.jpeg",
      "url": "",
      "token": "MTYzNF82ZTNkOTNlNWQ5YmUwM2MyZDcwM2I2MjBlMTQ3ZWMyZA==",
      "duration": 1254,
      "backgroundImage": "",
      "episodeNum": 0
    }
  ]
}
""".trimIndent()

fun main(args: Array<String>) {
    val resp = Json.parse<Response<List<AppColumnVO>>>(jsonString)
    println(resp.code)
    println(resp.data.size)

    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            path = "/test/writerError"

            asyncCompleteHandler {
                header {
                    HttpHeader.CONTENT_TYPE to MimeTypes.Type.APPLICATION_JSON_UTF_8.asString()
                }
                GlobalScope.async {
                    delay(50)
                    val ret = Json.toJson(resp)
                    println(ret)
                    println(ret.length)
                    write(ret)
                }.await().end()
            }
        }
    }.enableSecureConnection().listen("localhost", 8080)
}