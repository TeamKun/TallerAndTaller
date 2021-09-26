# TallerAndTaller
プレイヤーの大きさが徐々に大きくなるMod
minecraft forge 1.16.5-36.2.0

## コマンド
* tallerandtaller  
  * start  
  * stop  
  * config  
      * show  
      * set  
         * \<configItem>  
              * \<value>  
  * setScale  
    * \<scale>  
      * \<player>  

## 設定項目
* defaultScale\<Float> (1.0)
初ログイン時や,プレイヤーが死んだ時に設定される大きさ
* maxScale\<Float> (32.0)
タイマーで大きくなっていくときの上限値
setScaleコマンドはこの値を無視する
* increasingScale\<Float> (1.0)
1回辺りの大きくなる度合い
* timeToBeTaller\<Integer> (60)
次に大きくなるまでの時間
