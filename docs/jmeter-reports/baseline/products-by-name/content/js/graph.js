/*
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
$(document).ready(function() {

    $(".click-title").mouseenter( function(    e){
        e.preventDefault();
        this.style.cursor="pointer";
    });
    $(".click-title").mousedown( function(event){
        event.preventDefault();
    });

    // Ugly code while this script is shared among several pages
    try{
        refreshHitsPerSecond(true);
    } catch(e){}
    try{
        refreshResponseTimeOverTime(true);
    } catch(e){}
    try{
        refreshResponseTimePercentiles();
    } catch(e){}
});


var responseTimePercentilesInfos = {
        data: {"result": {"minY": 264.0, "minX": 0.0, "maxY": 7368.0, "series": [{"data": [[0.0, 264.0], [0.1, 290.0], [0.2, 293.0], [0.3, 315.0], [0.4, 317.0], [0.5, 345.0], [0.6, 362.0], [0.7, 372.0], [0.8, 377.0], [0.9, 379.0], [1.0, 384.0], [1.1, 390.0], [1.2, 407.0], [1.3, 412.0], [1.4, 425.0], [1.5, 438.0], [1.6, 444.0], [1.7, 465.0], [1.8, 475.0], [1.9, 486.0], [2.0, 491.0], [2.1, 520.0], [2.2, 541.0], [2.3, 610.0], [2.4, 652.0], [2.5, 686.0], [2.6, 709.0], [2.7, 710.0], [2.8, 752.0], [2.9, 762.0], [3.0, 773.0], [3.1, 774.0], [3.2, 794.0], [3.3, 830.0], [3.4, 852.0], [3.5, 862.0], [3.6, 867.0], [3.7, 885.0], [3.8, 940.0], [3.9, 953.0], [4.0, 1047.0], [4.1, 1047.0], [4.2, 1071.0], [4.3, 1087.0], [4.4, 1102.0], [4.5, 1128.0], [4.6, 1168.0], [4.7, 1186.0], [4.8, 1191.0], [4.9, 1238.0], [5.0, 1265.0], [5.1, 1281.0], [5.2, 1328.0], [5.3, 1337.0], [5.4, 1363.0], [5.5, 1375.0], [5.6, 1399.0], [5.7, 1400.0], [5.8, 1407.0], [5.9, 1420.0], [6.0, 1464.0], [6.1, 1469.0], [6.2, 1518.0], [6.3, 1538.0], [6.4, 1544.0], [6.5, 1601.0], [6.6, 1603.0], [6.7, 1618.0], [6.8, 1619.0], [6.9, 1657.0], [7.0, 1664.0], [7.1, 1666.0], [7.2, 1674.0], [7.3, 1677.0], [7.4, 1689.0], [7.5, 1724.0], [7.6, 1779.0], [7.7, 1779.0], [7.8, 1784.0], [7.9, 1796.0], [8.0, 1866.0], [8.1, 1895.0], [8.2, 1896.0], [8.3, 1917.0], [8.4, 1922.0], [8.5, 1935.0], [8.6, 1959.0], [8.7, 1988.0], [8.8, 2015.0], [8.9, 2017.0], [9.0, 2054.0], [9.1, 2064.0], [9.2, 2101.0], [9.3, 2110.0], [9.4, 2128.0], [9.5, 2138.0], [9.6, 2153.0], [9.7, 2217.0], [9.8, 2239.0], [9.9, 2260.0], [10.0, 2271.0], [10.1, 2293.0], [10.2, 2336.0], [10.3, 2351.0], [10.4, 2391.0], [10.5, 2393.0], [10.6, 2432.0], [10.7, 2454.0], [10.8, 2462.0], [10.9, 2486.0], [11.0, 2489.0], [11.1, 2506.0], [11.2, 2522.0], [11.3, 2536.0], [11.4, 2544.0], [11.5, 2582.0], [11.6, 2585.0], [11.7, 2649.0], [11.8, 2658.0], [11.9, 2715.0], [12.0, 2774.0], [12.1, 2778.0], [12.2, 2784.0], [12.3, 2790.0], [12.4, 2799.0], [12.5, 2804.0], [12.6, 2809.0], [12.7, 2853.0], [12.8, 2860.0], [12.9, 2865.0], [13.0, 2870.0], [13.1, 2900.0], [13.2, 2949.0], [13.3, 2973.0], [13.4, 2998.0], [13.5, 2999.0], [13.6, 3028.0], [13.7, 3080.0], [13.8, 3100.0], [13.9, 3105.0], [14.0, 3120.0], [14.1, 3123.0], [14.2, 3133.0], [14.3, 3172.0], [14.4, 3205.0], [14.5, 3266.0], [14.6, 3297.0], [14.7, 3305.0], [14.8, 3374.0], [14.9, 3379.0], [15.0, 3386.0], [15.1, 3394.0], [15.2, 3420.0], [15.3, 3424.0], [15.4, 3439.0], [15.5, 3460.0], [15.6, 3462.0], [15.7, 3497.0], [15.8, 3509.0], [15.9, 3526.0], [16.0, 3543.0], [16.1, 3571.0], [16.2, 3581.0], [16.3, 3608.0], [16.4, 3631.0], [16.5, 3639.0], [16.6, 3651.0], [16.7, 3658.0], [16.8, 3676.0], [16.9, 3678.0], [17.0, 3680.0], [17.1, 3692.0], [17.2, 3694.0], [17.3, 3697.0], [17.4, 3699.0], [17.5, 3702.0], [17.6, 3702.0], [17.7, 3704.0], [17.8, 3708.0], [17.9, 3710.0], [18.0, 3712.0], [18.1, 3720.0], [18.2, 3722.0], [18.3, 3724.0], [18.4, 3726.0], [18.5, 3726.0], [18.6, 3728.0], [18.7, 3730.0], [18.8, 3733.0], [18.9, 3742.0], [19.0, 3742.0], [19.1, 3743.0], [19.2, 3743.0], [19.3, 3746.0], [19.4, 3748.0], [19.5, 3748.0], [19.6, 3749.0], [19.7, 3753.0], [19.8, 3754.0], [19.9, 3755.0], [20.0, 3758.0], [20.1, 3761.0], [20.2, 3761.0], [20.3, 3763.0], [20.4, 3764.0], [20.5, 3766.0], [20.6, 3768.0], [20.7, 3772.0], [20.8, 3773.0], [20.9, 3773.0], [21.0, 3775.0], [21.1, 3776.0], [21.2, 3777.0], [21.3, 3778.0], [21.4, 3781.0], [21.5, 3782.0], [21.6, 3783.0], [21.7, 3791.0], [21.8, 3791.0], [21.9, 3795.0], [22.0, 3795.0], [22.1, 3797.0], [22.2, 3797.0], [22.3, 3798.0], [22.4, 3799.0], [22.5, 3799.0], [22.6, 3800.0], [22.7, 3800.0], [22.8, 3801.0], [22.9, 3801.0], [23.0, 3805.0], [23.1, 3805.0], [23.2, 3805.0], [23.3, 3809.0], [23.4, 3810.0], [23.5, 3811.0], [23.6, 3812.0], [23.7, 3814.0], [23.8, 3816.0], [23.9, 3816.0], [24.0, 3816.0], [24.1, 3817.0], [24.2, 3818.0], [24.3, 3818.0], [24.4, 3820.0], [24.5, 3821.0], [24.6, 3821.0], [24.7, 3821.0], [24.8, 3823.0], [24.9, 3825.0], [25.0, 3826.0], [25.1, 3826.0], [25.2, 3827.0], [25.3, 3829.0], [25.4, 3829.0], [25.5, 3830.0], [25.6, 3830.0], [25.7, 3830.0], [25.8, 3831.0], [25.9, 3832.0], [26.0, 3834.0], [26.1, 3835.0], [26.2, 3836.0], [26.3, 3836.0], [26.4, 3838.0], [26.5, 3840.0], [26.6, 3840.0], [26.7, 3841.0], [26.8, 3841.0], [26.9, 3843.0], [27.0, 3845.0], [27.1, 3846.0], [27.2, 3846.0], [27.3, 3847.0], [27.4, 3848.0], [27.5, 3848.0], [27.6, 3850.0], [27.7, 3851.0], [27.8, 3852.0], [27.9, 3853.0], [28.0, 3853.0], [28.1, 3855.0], [28.2, 3855.0], [28.3, 3856.0], [28.4, 3856.0], [28.5, 3857.0], [28.6, 3857.0], [28.7, 3858.0], [28.8, 3859.0], [28.9, 3860.0], [29.0, 3860.0], [29.1, 3861.0], [29.2, 3862.0], [29.3, 3862.0], [29.4, 3863.0], [29.5, 3863.0], [29.6, 3864.0], [29.7, 3864.0], [29.8, 3864.0], [29.9, 3866.0], [30.0, 3867.0], [30.1, 3867.0], [30.2, 3870.0], [30.3, 3870.0], [30.4, 3873.0], [30.5, 3874.0], [30.6, 3875.0], [30.7, 3875.0], [30.8, 3876.0], [30.9, 3876.0], [31.0, 3877.0], [31.1, 3878.0], [31.2, 3878.0], [31.3, 3880.0], [31.4, 3881.0], [31.5, 3882.0], [31.6, 3883.0], [31.7, 3883.0], [31.8, 3883.0], [31.9, 3883.0], [32.0, 3885.0], [32.1, 3886.0], [32.2, 3887.0], [32.3, 3887.0], [32.4, 3888.0], [32.5, 3891.0], [32.6, 3891.0], [32.7, 3893.0], [32.8, 3893.0], [32.9, 3894.0], [33.0, 3894.0], [33.1, 3895.0], [33.2, 3897.0], [33.3, 3897.0], [33.4, 3897.0], [33.5, 3897.0], [33.6, 3899.0], [33.7, 3899.0], [33.8, 3901.0], [33.9, 3901.0], [34.0, 3901.0], [34.1, 3902.0], [34.2, 3902.0], [34.3, 3902.0], [34.4, 3903.0], [34.5, 3903.0], [34.6, 3903.0], [34.7, 3903.0], [34.8, 3904.0], [34.9, 3905.0], [35.0, 3905.0], [35.1, 3906.0], [35.2, 3907.0], [35.3, 3907.0], [35.4, 3909.0], [35.5, 3909.0], [35.6, 3910.0], [35.7, 3911.0], [35.8, 3911.0], [35.9, 3912.0], [36.0, 3912.0], [36.1, 3913.0], [36.2, 3913.0], [36.3, 3914.0], [36.4, 3915.0], [36.5, 3915.0], [36.6, 3916.0], [36.7, 3916.0], [36.8, 3918.0], [36.9, 3918.0], [37.0, 3918.0], [37.1, 3919.0], [37.2, 3919.0], [37.3, 3923.0], [37.4, 3924.0], [37.5, 3925.0], [37.6, 3925.0], [37.7, 3925.0], [37.8, 3926.0], [37.9, 3927.0], [38.0, 3929.0], [38.1, 3930.0], [38.2, 3932.0], [38.3, 3933.0], [38.4, 3934.0], [38.5, 3934.0], [38.6, 3935.0], [38.7, 3936.0], [38.8, 3937.0], [38.9, 3937.0], [39.0, 3937.0], [39.1, 3938.0], [39.2, 3938.0], [39.3, 3938.0], [39.4, 3939.0], [39.5, 3941.0], [39.6, 3943.0], [39.7, 3943.0], [39.8, 3943.0], [39.9, 3943.0], [40.0, 3943.0], [40.1, 3943.0], [40.2, 3945.0], [40.3, 3945.0], [40.4, 3946.0], [40.5, 3947.0], [40.6, 3947.0], [40.7, 3948.0], [40.8, 3948.0], [40.9, 3949.0], [41.0, 3950.0], [41.1, 3950.0], [41.2, 3952.0], [41.3, 3953.0], [41.4, 3954.0], [41.5, 3955.0], [41.6, 3955.0], [41.7, 3958.0], [41.8, 3960.0], [41.9, 3961.0], [42.0, 3961.0], [42.1, 3963.0], [42.2, 3963.0], [42.3, 3964.0], [42.4, 3964.0], [42.5, 3965.0], [42.6, 3966.0], [42.7, 3967.0], [42.8, 3967.0], [42.9, 3967.0], [43.0, 3969.0], [43.1, 3969.0], [43.2, 3970.0], [43.3, 3971.0], [43.4, 3972.0], [43.5, 3973.0], [43.6, 3973.0], [43.7, 3974.0], [43.8, 3974.0], [43.9, 3974.0], [44.0, 3976.0], [44.1, 3976.0], [44.2, 3977.0], [44.3, 3977.0], [44.4, 3978.0], [44.5, 3979.0], [44.6, 3979.0], [44.7, 3980.0], [44.8, 3980.0], [44.9, 3981.0], [45.0, 3982.0], [45.1, 3983.0], [45.2, 3983.0], [45.3, 3984.0], [45.4, 3985.0], [45.5, 3985.0], [45.6, 3987.0], [45.7, 3988.0], [45.8, 3989.0], [45.9, 3989.0], [46.0, 3990.0], [46.1, 3991.0], [46.2, 3993.0], [46.3, 3994.0], [46.4, 3995.0], [46.5, 4000.0], [46.6, 4000.0], [46.7, 4002.0], [46.8, 4003.0], [46.9, 4003.0], [47.0, 4003.0], [47.1, 4003.0], [47.2, 4008.0], [47.3, 4009.0], [47.4, 4010.0], [47.5, 4010.0], [47.6, 4011.0], [47.7, 4012.0], [47.8, 4012.0], [47.9, 4015.0], [48.0, 4017.0], [48.1, 4018.0], [48.2, 4019.0], [48.3, 4021.0], [48.4, 4022.0], [48.5, 4022.0], [48.6, 4022.0], [48.7, 4025.0], [48.8, 4026.0], [48.9, 4026.0], [49.0, 4028.0], [49.1, 4028.0], [49.2, 4030.0], [49.3, 4032.0], [49.4, 4032.0], [49.5, 4037.0], [49.6, 4037.0], [49.7, 4040.0], [49.8, 4041.0], [49.9, 4043.0], [50.0, 4043.0], [50.1, 4044.0], [50.2, 4045.0], [50.3, 4045.0], [50.4, 4047.0], [50.5, 4048.0], [50.6, 4048.0], [50.7, 4049.0], [50.8, 4050.0], [50.9, 4052.0], [51.0, 4052.0], [51.1, 4055.0], [51.2, 4057.0], [51.3, 4057.0], [51.4, 4058.0], [51.5, 4060.0], [51.6, 4062.0], [51.7, 4063.0], [51.8, 4064.0], [51.9, 4064.0], [52.0, 4065.0], [52.1, 4066.0], [52.2, 4066.0], [52.3, 4067.0], [52.4, 4067.0], [52.5, 4069.0], [52.6, 4069.0], [52.7, 4070.0], [52.8, 4070.0], [52.9, 4073.0], [53.0, 4073.0], [53.1, 4074.0], [53.2, 4076.0], [53.3, 4076.0], [53.4, 4077.0], [53.5, 4078.0], [53.6, 4080.0], [53.7, 4080.0], [53.8, 4082.0], [53.9, 4083.0], [54.0, 4084.0], [54.1, 4084.0], [54.2, 4087.0], [54.3, 4087.0], [54.4, 4087.0], [54.5, 4089.0], [54.6, 4089.0], [54.7, 4089.0], [54.8, 4090.0], [54.9, 4090.0], [55.0, 4092.0], [55.1, 4093.0], [55.2, 4098.0], [55.3, 4099.0], [55.4, 4099.0], [55.5, 4101.0], [55.6, 4103.0], [55.7, 4105.0], [55.8, 4105.0], [55.9, 4107.0], [56.0, 4107.0], [56.1, 4108.0], [56.2, 4108.0], [56.3, 4109.0], [56.4, 4110.0], [56.5, 4111.0], [56.6, 4112.0], [56.7, 4112.0], [56.8, 4113.0], [56.9, 4115.0], [57.0, 4115.0], [57.1, 4116.0], [57.2, 4117.0], [57.3, 4122.0], [57.4, 4122.0], [57.5, 4124.0], [57.6, 4125.0], [57.7, 4126.0], [57.8, 4129.0], [57.9, 4129.0], [58.0, 4131.0], [58.1, 4133.0], [58.2, 4134.0], [58.3, 4136.0], [58.4, 4136.0], [58.5, 4138.0], [58.6, 4138.0], [58.7, 4139.0], [58.8, 4142.0], [58.9, 4145.0], [59.0, 4145.0], [59.1, 4148.0], [59.2, 4148.0], [59.3, 4149.0], [59.4, 4150.0], [59.5, 4151.0], [59.6, 4153.0], [59.7, 4153.0], [59.8, 4155.0], [59.9, 4155.0], [60.0, 4158.0], [60.1, 4159.0], [60.2, 4159.0], [60.3, 4161.0], [60.4, 4163.0], [60.5, 4164.0], [60.6, 4166.0], [60.7, 4167.0], [60.8, 4167.0], [60.9, 4168.0], [61.0, 4169.0], [61.1, 4171.0], [61.2, 4176.0], [61.3, 4177.0], [61.4, 4178.0], [61.5, 4179.0], [61.6, 4182.0], [61.7, 4183.0], [61.8, 4185.0], [61.9, 4189.0], [62.0, 4189.0], [62.1, 4191.0], [62.2, 4193.0], [62.3, 4194.0], [62.4, 4196.0], [62.5, 4198.0], [62.6, 4203.0], [62.7, 4203.0], [62.8, 4206.0], [62.9, 4213.0], [63.0, 4214.0], [63.1, 4215.0], [63.2, 4221.0], [63.3, 4221.0], [63.4, 4223.0], [63.5, 4226.0], [63.6, 4227.0], [63.7, 4227.0], [63.8, 4233.0], [63.9, 4234.0], [64.0, 4240.0], [64.1, 4241.0], [64.2, 4248.0], [64.3, 4250.0], [64.4, 4258.0], [64.5, 4259.0], [64.6, 4261.0], [64.7, 4261.0], [64.8, 4264.0], [64.9, 4269.0], [65.0, 4270.0], [65.1, 4271.0], [65.2, 4273.0], [65.3, 4276.0], [65.4, 4277.0], [65.5, 4278.0], [65.6, 4279.0], [65.7, 4282.0], [65.8, 4283.0], [65.9, 4283.0], [66.0, 4285.0], [66.1, 4285.0], [66.2, 4286.0], [66.3, 4294.0], [66.4, 4295.0], [66.5, 4299.0], [66.6, 4300.0], [66.7, 4304.0], [66.8, 4305.0], [66.9, 4308.0], [67.0, 4308.0], [67.1, 4312.0], [67.2, 4317.0], [67.3, 4318.0], [67.4, 4323.0], [67.5, 4326.0], [67.6, 4329.0], [67.7, 4334.0], [67.8, 4336.0], [67.9, 4343.0], [68.0, 4344.0], [68.1, 4345.0], [68.2, 4346.0], [68.3, 4351.0], [68.4, 4357.0], [68.5, 4362.0], [68.6, 4366.0], [68.7, 4369.0], [68.8, 4375.0], [68.9, 4390.0], [69.0, 4392.0], [69.1, 4409.0], [69.2, 4411.0], [69.3, 4420.0], [69.4, 4421.0], [69.5, 4431.0], [69.6, 4435.0], [69.7, 4456.0], [69.8, 4462.0], [69.9, 4486.0], [70.0, 4487.0], [70.1, 4504.0], [70.2, 4528.0], [70.3, 4536.0], [70.4, 4538.0], [70.5, 4543.0], [70.6, 4557.0], [70.7, 4581.0], [70.8, 4593.0], [70.9, 4594.0], [71.0, 4596.0], [71.1, 4598.0], [71.2, 4607.0], [71.3, 4615.0], [71.4, 4616.0], [71.5, 4622.0], [71.6, 4622.0], [71.7, 4626.0], [71.8, 4633.0], [71.9, 4642.0], [72.0, 4648.0], [72.1, 4649.0], [72.2, 4651.0], [72.3, 4654.0], [72.4, 4657.0], [72.5, 4660.0], [72.6, 4666.0], [72.7, 4678.0], [72.8, 4680.0], [72.9, 4682.0], [73.0, 4684.0], [73.1, 4685.0], [73.2, 4690.0], [73.3, 4690.0], [73.4, 4691.0], [73.5, 4691.0], [73.6, 4693.0], [73.7, 4695.0], [73.8, 4697.0], [73.9, 4698.0], [74.0, 4699.0], [74.1, 4703.0], [74.2, 4705.0], [74.3, 4709.0], [74.4, 4709.0], [74.5, 4711.0], [74.6, 4712.0], [74.7, 4713.0], [74.8, 4716.0], [74.9, 4717.0], [75.0, 4718.0], [75.1, 4718.0], [75.2, 4720.0], [75.3, 4721.0], [75.4, 4722.0], [75.5, 4725.0], [75.6, 4727.0], [75.7, 4728.0], [75.8, 4728.0], [75.9, 4729.0], [76.0, 4730.0], [76.1, 4730.0], [76.2, 4731.0], [76.3, 4732.0], [76.4, 4736.0], [76.5, 4737.0], [76.6, 4739.0], [76.7, 4740.0], [76.8, 4742.0], [76.9, 4742.0], [77.0, 4743.0], [77.1, 4744.0], [77.2, 4746.0], [77.3, 4749.0], [77.4, 4750.0], [77.5, 4751.0], [77.6, 4752.0], [77.7, 4754.0], [77.8, 4758.0], [77.9, 4758.0], [78.0, 4761.0], [78.1, 4762.0], [78.2, 4764.0], [78.3, 4765.0], [78.4, 4767.0], [78.5, 4768.0], [78.6, 4768.0], [78.7, 4770.0], [78.8, 4773.0], [78.9, 4774.0], [79.0, 4775.0], [79.1, 4776.0], [79.2, 4777.0], [79.3, 4777.0], [79.4, 4778.0], [79.5, 4778.0], [79.6, 4780.0], [79.7, 4781.0], [79.8, 4782.0], [79.9, 4782.0], [80.0, 4784.0], [80.1, 4784.0], [80.2, 4785.0], [80.3, 4787.0], [80.4, 4787.0], [80.5, 4788.0], [80.6, 4789.0], [80.7, 4789.0], [80.8, 4792.0], [80.9, 4792.0], [81.0, 4793.0], [81.1, 4794.0], [81.2, 4798.0], [81.3, 4799.0], [81.4, 4802.0], [81.5, 4802.0], [81.6, 4802.0], [81.7, 4803.0], [81.8, 4806.0], [81.9, 4807.0], [82.0, 4807.0], [82.1, 4809.0], [82.2, 4811.0], [82.3, 4813.0], [82.4, 4813.0], [82.5, 4814.0], [82.6, 4814.0], [82.7, 4816.0], [82.8, 4817.0], [82.9, 4819.0], [83.0, 4822.0], [83.1, 4822.0], [83.2, 4824.0], [83.3, 4826.0], [83.4, 4827.0], [83.5, 4828.0], [83.6, 4829.0], [83.7, 4830.0], [83.8, 4834.0], [83.9, 4834.0], [84.0, 4836.0], [84.1, 4836.0], [84.2, 4837.0], [84.3, 4837.0], [84.4, 4838.0], [84.5, 4841.0], [84.6, 4842.0], [84.7, 4845.0], [84.8, 4845.0], [84.9, 4846.0], [85.0, 4847.0], [85.1, 4851.0], [85.2, 4852.0], [85.3, 4854.0], [85.4, 4855.0], [85.5, 4857.0], [85.6, 4859.0], [85.7, 4861.0], [85.8, 4861.0], [85.9, 4864.0], [86.0, 4864.0], [86.1, 4865.0], [86.2, 4865.0], [86.3, 4865.0], [86.4, 4865.0], [86.5, 4868.0], [86.6, 4868.0], [86.7, 4872.0], [86.8, 4873.0], [86.9, 4873.0], [87.0, 4873.0], [87.1, 4875.0], [87.2, 4876.0], [87.3, 4879.0], [87.4, 4880.0], [87.5, 4886.0], [87.6, 4888.0], [87.7, 4888.0], [87.8, 4889.0], [87.9, 4890.0], [88.0, 4890.0], [88.1, 4891.0], [88.2, 4891.0], [88.3, 4895.0], [88.4, 4895.0], [88.5, 4896.0], [88.6, 4898.0], [88.7, 4898.0], [88.8, 4900.0], [88.9, 4901.0], [89.0, 4903.0], [89.1, 4904.0], [89.2, 4910.0], [89.3, 4911.0], [89.4, 4911.0], [89.5, 4915.0], [89.6, 4915.0], [89.7, 4917.0], [89.8, 4919.0], [89.9, 4920.0], [90.0, 4923.0], [90.1, 4923.0], [90.2, 4925.0], [90.3, 4925.0], [90.4, 4927.0], [90.5, 4930.0], [90.6, 4935.0], [90.7, 4936.0], [90.8, 4936.0], [90.9, 4939.0], [91.0, 4940.0], [91.1, 4942.0], [91.2, 4944.0], [91.3, 4949.0], [91.4, 4953.0], [91.5, 4954.0], [91.6, 4957.0], [91.7, 4959.0], [91.8, 4960.0], [91.9, 4960.0], [92.0, 4964.0], [92.1, 4964.0], [92.2, 4971.0], [92.3, 4972.0], [92.4, 4973.0], [92.5, 4974.0], [92.6, 4976.0], [92.7, 4976.0], [92.8, 4981.0], [92.9, 4983.0], [93.0, 4985.0], [93.1, 4987.0], [93.2, 4988.0], [93.3, 4988.0], [93.4, 4989.0], [93.5, 4994.0], [93.6, 4995.0], [93.7, 4997.0], [93.8, 4999.0], [93.9, 4999.0], [94.0, 5001.0], [94.1, 5004.0], [94.2, 5005.0], [94.3, 5007.0], [94.4, 5008.0], [94.5, 5010.0], [94.6, 5010.0], [94.7, 5012.0], [94.8, 5014.0], [94.9, 5018.0], [95.0, 5019.0], [95.1, 5024.0], [95.2, 5024.0], [95.3, 5026.0], [95.4, 5028.0], [95.5, 5038.0], [95.6, 5039.0], [95.7, 5040.0], [95.8, 5042.0], [95.9, 5055.0], [96.0, 5055.0], [96.1, 5058.0], [96.2, 5062.0], [96.3, 5063.0], [96.4, 5068.0], [96.5, 5070.0], [96.6, 5078.0], [96.7, 5078.0], [96.8, 5084.0], [96.9, 5089.0], [97.0, 5095.0], [97.1, 5095.0], [97.2, 5097.0], [97.3, 5102.0], [97.4, 5103.0], [97.5, 5105.0], [97.6, 5109.0], [97.7, 5109.0], [97.8, 5117.0], [97.9, 5118.0], [98.0, 5119.0], [98.1, 5119.0], [98.2, 5130.0], [98.3, 5131.0], [98.4, 5141.0], [98.5, 5155.0], [98.6, 5158.0], [98.7, 5171.0], [98.8, 5192.0], [98.9, 5195.0], [99.0, 5197.0], [99.1, 5217.0], [99.2, 5219.0], [99.3, 5228.0], [99.4, 5243.0], [99.5, 5248.0], [99.6, 5318.0], [99.7, 5353.0], [99.8, 5435.0], [99.9, 5455.0], [100.0, 7368.0]], "isOverall": false, "label": "GET /products by name", "isController": false}], "supportsControllersDiscrimination": true, "maxX": 100.0, "title": "Response Time Percentiles"}},
        getOptions: function() {
            return {
                series: {
                    points: { show: false }
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: '#legendResponseTimePercentiles'
                },
                xaxis: {
                    tickDecimals: 1,
                    axisLabel: "Percentiles",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Percentile value in ms",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s : %x.2 percentile was %y ms"
                },
                selection: { mode: "xy" },
            };
        },
        createGraph: function() {
            var data = this.data;
            var dataset = prepareData(data.result.series, $("#choicesResponseTimePercentiles"));
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotResponseTimesPercentiles"), dataset, options);
            // setup overview
            $.plot($("#overviewResponseTimesPercentiles"), dataset, prepareOverviewOptions(options));
        }
};

/**
 * @param elementId Id of element where we display message
 */
function setEmptyGraph(elementId) {
    $(function() {
        $(elementId).text("No graph series with filter="+seriesFilter);
    });
}

// Response times percentiles
function refreshResponseTimePercentiles() {
    var infos = responseTimePercentilesInfos;
    prepareSeries(infos.data);
    if(infos.data.result.series.length == 0) {
        setEmptyGraph("#bodyResponseTimePercentiles");
        return;
    }
    if (isGraph($("#flotResponseTimesPercentiles"))){
        infos.createGraph();
    } else {
        var choiceContainer = $("#choicesResponseTimePercentiles");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotResponseTimesPercentiles", "#overviewResponseTimesPercentiles");
        $('#bodyResponseTimePercentiles .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
}

var responseTimeDistributionInfos = {
        data: {"result": {"minY": 1.0, "minX": 200.0, "maxY": 183.0, "series": [{"data": [[600.0, 3.0], [700.0, 11.0], [800.0, 7.0], [900.0, 3.0], [1000.0, 6.0], [1100.0, 7.0], [1200.0, 4.0], [1300.0, 7.0], [1400.0, 7.0], [1500.0, 5.0], [1600.0, 14.0], [1700.0, 7.0], [1800.0, 4.0], [1900.0, 7.0], [2000.0, 7.0], [2100.0, 7.0], [2200.0, 6.0], [2300.0, 6.0], [2400.0, 7.0], [2500.0, 9.0], [2600.0, 3.0], [2700.0, 8.0], [2800.0, 9.0], [2900.0, 7.0], [3000.0, 4.0], [3100.0, 8.0], [3200.0, 4.0], [3300.0, 8.0], [3400.0, 8.0], [3500.0, 7.0], [3700.0, 72.0], [3600.0, 18.0], [3800.0, 161.0], [3900.0, 183.0], [4000.0, 129.0], [4100.0, 102.0], [4200.0, 57.0], [4300.0, 36.0], [4400.0, 14.0], [4500.0, 16.0], [4600.0, 41.0], [4700.0, 105.0], [4800.0, 107.0], [4900.0, 74.0], [5000.0, 48.0], [5100.0, 25.0], [5200.0, 8.0], [5300.0, 2.0], [5400.0, 3.0], [7300.0, 1.0], [200.0, 3.0], [300.0, 14.0], [400.0, 12.0], [500.0, 4.0]], "isOverall": false, "label": "GET /products by name", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 100, "maxX": 7300.0, "title": "Response Time Distribution"}},
        getOptions: function() {
            var granularity = this.data.result.granularity;
            return {
                legend: {
                    noColumns: 2,
                    show: true,
                    container: '#legendResponseTimeDistribution'
                },
                xaxis:{
                    axisLabel: "Response times in ms",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Number of responses",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                bars : {
                    show: true,
                    barWidth: this.data.result.granularity
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: function(label, xval, yval, flotItem){
                        return yval + " responses for " + label + " were between " + xval + " and " + (xval + granularity) + " ms";
                    }
                }
            };
        },
        createGraph: function() {
            var data = this.data;
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotResponseTimeDistribution"), prepareData(data.result.series, $("#choicesResponseTimeDistribution")), options);
        }

};

// Response time distribution
function refreshResponseTimeDistribution() {
    var infos = responseTimeDistributionInfos;
    prepareSeries(infos.data);
    if(infos.data.result.series.length == 0) {
        setEmptyGraph("#bodyResponseTimeDistribution");
        return;
    }
    if (isGraph($("#flotResponseTimeDistribution"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesResponseTimeDistribution");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        $('#footerResponseTimeDistribution .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};


var syntheticResponseTimeDistributionInfos = {
        data: {"result": {"minY": 30.0, "minX": 0.0, "ticks": [[0, "Requests having \nresponse time <= 500ms"], [1, "Requests having \nresponse time > 500ms and <= 1,500ms"], [2, "Requests having \nresponse time > 1,500ms"], [3, "Requests in error"]], "maxY": 1347.0, "series": [{"data": [[0.0, 30.0]], "color": "#9ACD32", "isOverall": false, "label": "Requests having \nresponse time <= 500ms", "isController": false}, {"data": [[1.0, 58.0]], "color": "yellow", "isOverall": false, "label": "Requests having \nresponse time > 500ms and <= 1,500ms", "isController": false}, {"data": [[2.0, 1347.0]], "color": "orange", "isOverall": false, "label": "Requests having \nresponse time > 1,500ms", "isController": false}, {"data": [], "color": "#FF6347", "isOverall": false, "label": "Requests in error", "isController": false}], "supportsControllersDiscrimination": false, "maxX": 2.0, "title": "Synthetic Response Times Distribution"}},
        getOptions: function() {
            return {
                legend: {
                    noColumns: 2,
                    show: true,
                    container: '#legendSyntheticResponseTimeDistribution'
                },
                xaxis:{
                    axisLabel: "Response times ranges",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                    tickLength:0,
                    min:-0.5,
                    max:3.5
                },
                yaxis: {
                    axisLabel: "Number of responses",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                bars : {
                    show: true,
                    align: "center",
                    barWidth: 0.25,
                    fill:.75
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: function(label, xval, yval, flotItem){
                        return yval + " " + label;
                    }
                }
            };
        },
        createGraph: function() {
            var data = this.data;
            var options = this.getOptions();
            prepareOptions(options, data);
            options.xaxis.ticks = data.result.ticks;
            $.plot($("#flotSyntheticResponseTimeDistribution"), prepareData(data.result.series, $("#choicesSyntheticResponseTimeDistribution")), options);
        }

};

// Response time distribution
function refreshSyntheticResponseTimeDistribution() {
    var infos = syntheticResponseTimeDistributionInfos;
    prepareSeries(infos.data, true);
    if (isGraph($("#flotSyntheticResponseTimeDistribution"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesSyntheticResponseTimeDistribution");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        $('#footerSyntheticResponseTimeDistribution .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};

var activeThreadsOverTimeInfos = {
        data: {"result": {"minY": 18.544217687074823, "minX": 1.78260024E12, "maxY": 49.12857142857145, "series": [{"data": [[1.7826003E12, 49.12857142857145], [1.78260024E12, 18.544217687074823], [1.78260036E12, 47.92006802721088]], "isOverall": false, "label": "Readers", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.78260036E12, "title": "Active Threads Over Time"}},
        getOptions: function() {
            return {
                series: {
                    stack: true,
                    lines: {
                        show: true,
                        fill: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getElapsedTimeLabel(this.data.result.granularity),
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Number of active threads",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20
                },
                legend: {
                    noColumns: 6,
                    show: true,
                    container: '#legendActiveThreadsOverTime'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                selection: {
                    mode: 'xy'
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s : At %x there were %y active threads"
                }
            };
        },
        createGraph: function() {
            var data = this.data;
            var dataset = prepareData(data.result.series, $("#choicesActiveThreadsOverTime"));
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotActiveThreadsOverTime"), dataset, options);
            // setup overview
            $.plot($("#overviewActiveThreadsOverTime"), dataset, prepareOverviewOptions(options));
        }
};

// Active Threads Over Time
function refreshActiveThreadsOverTime(fixTimestamps) {
    var infos = activeThreadsOverTimeInfos;
    prepareSeries(infos.data);
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, -10800000);
    }
    if(isGraph($("#flotActiveThreadsOverTime"))) {
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesActiveThreadsOverTime");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotActiveThreadsOverTime", "#overviewActiveThreadsOverTime");
        $('#footerActiveThreadsOverTime .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};

var timeVsThreadsInfos = {
        data: {"result": {"minY": 304.3333333333333, "minX": 1.0, "maxY": 4594.0, "series": [{"data": [[2.0, 1441.75], [3.0, 304.3333333333333], [4.0, 1850.8], [5.0, 1231.6], [6.0, 1156.6], [7.0, 1465.6], [8.0, 1330.8333333333335], [9.0, 1223.0], [10.0, 1356.6], [11.0, 1298.7142857142858], [12.0, 2015.75], [13.0, 1286.142857142857], [14.0, 1933.2], [15.0, 1366.6666666666667], [16.0, 1566.5714285714284], [17.0, 1704.75], [18.0, 1867.0], [19.0, 1866.1666666666665], [20.0, 1845.8], [21.0, 1890.0], [22.0, 2020.4285714285716], [23.0, 2190.1666666666665], [24.0, 2280.5], [25.0, 2163.714285714286], [26.0, 2194.714285714286], [27.0, 2319.25], [28.0, 2406.6], [29.0, 2319.0], [30.0, 2479.25], [31.0, 2535.6666666666665], [32.0, 2560.714285714286], [33.0, 2517.1666666666665], [34.0, 1579.3333333333333], [35.0, 3035.0], [36.0, 2810.25], [37.0, 2744.0], [38.0, 2899.6], [39.0, 3076.5], [40.0, 3210.8333333333335], [41.0, 3459.6], [42.0, 3086.5714285714284], [43.0, 3337.166666666667], [44.0, 3218.4], [45.0, 3258.5], [46.0, 3333.5], [47.0, 3436.3333333333335], [48.0, 3769.4], [49.0, 3425.0], [50.0, 4291.196397941681], [1.0, 4594.0]], "isOverall": false, "label": "GET /products by name", "isController": false}, {"data": [[45.500348432055745, 3915.5094076655055]], "isOverall": false, "label": "GET /products by name-Aggregated", "isController": false}], "supportsControllersDiscrimination": true, "maxX": 50.0, "title": "Time VS Threads"}},
        getOptions: function() {
            return {
                series: {
                    lines: {
                        show: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    axisLabel: "Number of active threads",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Average response times in ms",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20
                },
                legend: { noColumns: 2,show: true, container: '#legendTimeVsThreads' },
                selection: {
                    mode: 'xy'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s: At %x.2 active threads, Average response time was %y.2 ms"
                }
            };
        },
        createGraph: function() {
            var data = this.data;
            var dataset = prepareData(data.result.series, $("#choicesTimeVsThreads"));
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotTimesVsThreads"), dataset, options);
            // setup overview
            $.plot($("#overviewTimesVsThreads"), dataset, prepareOverviewOptions(options));
        }
};

// Time vs threads
function refreshTimeVsThreads(){
    var infos = timeVsThreadsInfos;
    prepareSeries(infos.data);
    if(infos.data.result.series.length == 0) {
        setEmptyGraph("#bodyTimeVsThreads");
        return;
    }
    if(isGraph($("#flotTimesVsThreads"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesTimeVsThreads");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotTimesVsThreads", "#overviewTimesVsThreads");
        $('#footerTimeVsThreads .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};

var bytesThroughputOverTimeInfos = {
        data : {"result": {"minY": 458.15, "minX": 1.78260024E12, "maxY": 6218.333333333333, "series": [{"data": [[1.7826003E12, 6218.333333333333], [1.78260024E12, 1305.85], [1.78260036E12, 5223.4]], "isOverall": false, "label": "Bytes received per second", "isController": false}, {"data": [[1.7826003E12, 2181.6666666666665], [1.78260024E12, 458.15], [1.78260036E12, 1832.6]], "isOverall": false, "label": "Bytes sent per second", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.78260036E12, "title": "Bytes Throughput Over Time"}},
        getOptions : function(){
            return {
                series: {
                    lines: {
                        show: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getElapsedTimeLabel(this.data.result.granularity) ,
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Bytes / sec",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: '#legendBytesThroughputOverTime'
                },
                selection: {
                    mode: "xy"
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s at %x was %y"
                }
            };
        },
        createGraph : function() {
            var data = this.data;
            var dataset = prepareData(data.result.series, $("#choicesBytesThroughputOverTime"));
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotBytesThroughputOverTime"), dataset, options);
            // setup overview
            $.plot($("#overviewBytesThroughputOverTime"), dataset, prepareOverviewOptions(options));
        }
};

// Bytes throughput Over Time
function refreshBytesThroughputOverTime(fixTimestamps) {
    var infos = bytesThroughputOverTimeInfos;
    prepareSeries(infos.data);
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, -10800000);
    }
    if(isGraph($("#flotBytesThroughputOverTime"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesBytesThroughputOverTime");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotBytesThroughputOverTime", "#overviewBytesThroughputOverTime");
        $('#footerBytesThroughputOverTime .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
}

var responseTimesOverTimeInfos = {
        data: {"result": {"minY": 1293.700680272109, "minX": 1.78260024E12, "maxY": 4362.4387755102025, "series": [{"data": [[1.7826003E12, 4090.66857142857], [1.78260024E12, 1293.700680272109], [1.78260036E12, 4362.4387755102025]], "isOverall": false, "label": "GET /products by name", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.78260036E12, "title": "Response Time Over Time"}},
        getOptions: function(){
            return {
                series: {
                    lines: {
                        show: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getElapsedTimeLabel(this.data.result.granularity),
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Average response time in ms",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: '#legendResponseTimesOverTime'
                },
                selection: {
                    mode: 'xy'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s : at %x Average response time was %y ms"
                }
            };
        },
        createGraph: function() {
            var data = this.data;
            var dataset = prepareData(data.result.series, $("#choicesResponseTimesOverTime"));
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotResponseTimesOverTime"), dataset, options);
            // setup overview
            $.plot($("#overviewResponseTimesOverTime"), dataset, prepareOverviewOptions(options));
        }
};

// Response Times Over Time
function refreshResponseTimeOverTime(fixTimestamps) {
    var infos = responseTimesOverTimeInfos;
    prepareSeries(infos.data);
    if(infos.data.result.series.length == 0) {
        setEmptyGraph("#bodyResponseTimeOverTime");
        return;
    }
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, -10800000);
    }
    if(isGraph($("#flotResponseTimesOverTime"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesResponseTimesOverTime");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotResponseTimesOverTime", "#overviewResponseTimesOverTime");
        $('#footerResponseTimesOverTime .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};

var latenciesOverTimeInfos = {
        data: {"result": {"minY": 1293.591836734694, "minX": 1.78260024E12, "maxY": 4362.426870748297, "series": [{"data": [[1.7826003E12, 4090.6442857142874], [1.78260024E12, 1293.591836734694], [1.78260036E12, 4362.426870748297]], "isOverall": false, "label": "GET /products by name", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.78260036E12, "title": "Latencies Over Time"}},
        getOptions: function() {
            return {
                series: {
                    lines: {
                        show: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getElapsedTimeLabel(this.data.result.granularity),
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Average response latencies in ms",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: '#legendLatenciesOverTime'
                },
                selection: {
                    mode: 'xy'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s : at %x Average latency was %y ms"
                }
            };
        },
        createGraph: function () {
            var data = this.data;
            var dataset = prepareData(data.result.series, $("#choicesLatenciesOverTime"));
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotLatenciesOverTime"), dataset, options);
            // setup overview
            $.plot($("#overviewLatenciesOverTime"), dataset, prepareOverviewOptions(options));
        }
};

// Latencies Over Time
function refreshLatenciesOverTime(fixTimestamps) {
    var infos = latenciesOverTimeInfos;
    prepareSeries(infos.data);
    if(infos.data.result.series.length == 0) {
        setEmptyGraph("#bodyLatenciesOverTime");
        return;
    }
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, -10800000);
    }
    if(isGraph($("#flotLatenciesOverTime"))) {
        infos.createGraph();
    }else {
        var choiceContainer = $("#choicesLatenciesOverTime");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotLatenciesOverTime", "#overviewLatenciesOverTime");
        $('#footerLatenciesOverTime .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};

var connectTimeOverTimeInfos = {
        data: {"result": {"minY": 0.017006802721088447, "minX": 1.78260024E12, "maxY": 0.08843537414965986, "series": [{"data": [[1.7826003E12, 0.025714285714285738], [1.78260024E12, 0.08843537414965986], [1.78260036E12, 0.017006802721088447]], "isOverall": false, "label": "GET /products by name", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.78260036E12, "title": "Connect Time Over Time"}},
        getOptions: function() {
            return {
                series: {
                    lines: {
                        show: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getConnectTimeLabel(this.data.result.granularity),
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Average Connect Time in ms",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: '#legendConnectTimeOverTime'
                },
                selection: {
                    mode: 'xy'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s : at %x Average connect time was %y ms"
                }
            };
        },
        createGraph: function () {
            var data = this.data;
            var dataset = prepareData(data.result.series, $("#choicesConnectTimeOverTime"));
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotConnectTimeOverTime"), dataset, options);
            // setup overview
            $.plot($("#overviewConnectTimeOverTime"), dataset, prepareOverviewOptions(options));
        }
};

// Connect Time Over Time
function refreshConnectTimeOverTime(fixTimestamps) {
    var infos = connectTimeOverTimeInfos;
    prepareSeries(infos.data);
    if(infos.data.result.series.length == 0) {
        setEmptyGraph("#bodyConnectTimeOverTime");
        return;
    }
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, -10800000);
    }
    if(isGraph($("#flotConnectTimeOverTime"))) {
        infos.createGraph();
    }else {
        var choiceContainer = $("#choicesConnectTimeOverTime");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotConnectTimeOverTime", "#overviewConnectTimeOverTime");
        $('#footerConnectTimeOverTime .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};

var responseTimePercentilesOverTimeInfos = {
        data: {"result": {"minY": 264.0, "minX": 1.78260024E12, "maxY": 7368.0, "series": [{"data": [[1.7826003E12, 5248.0], [1.78260024E12, 2790.0], [1.78260036E12, 7368.0]], "isOverall": false, "label": "Max", "isController": false}, {"data": [[1.7826003E12, 762.0], [1.78260024E12, 264.0], [1.78260036E12, 1399.0]], "isOverall": false, "label": "Min", "isController": false}, {"data": [[1.7826003E12, 4872.9], [1.78260024E12, 2262.2000000000003], [1.78260036E12, 5019.5]], "isOverall": false, "label": "90th percentile", "isController": false}, {"data": [[1.7826003E12, 5094.8], [1.78260024E12, 2785.6800000000003], [1.78260036E12, 5321.85]], "isOverall": false, "label": "99th percentile", "isController": false}, {"data": [[1.7826003E12, 3986.0], [1.78260024E12, 1328.0], [1.78260036E12, 4185.5]], "isOverall": false, "label": "Median", "isController": false}, {"data": [[1.7826003E12, 4956.849999999999], [1.78260024E12, 2499.2], [1.78260036E12, 5109.0]], "isOverall": false, "label": "95th percentile", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.78260036E12, "title": "Response Time Percentiles Over Time (successful requests only)"}},
        getOptions: function() {
            return {
                series: {
                    lines: {
                        show: true,
                        fill: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getElapsedTimeLabel(this.data.result.granularity),
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Response Time in ms",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: '#legendResponseTimePercentilesOverTime'
                },
                selection: {
                    mode: 'xy'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s : at %x Response time was %y ms"
                }
            };
        },
        createGraph: function () {
            var data = this.data;
            var dataset = prepareData(data.result.series, $("#choicesResponseTimePercentilesOverTime"));
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotResponseTimePercentilesOverTime"), dataset, options);
            // setup overview
            $.plot($("#overviewResponseTimePercentilesOverTime"), dataset, prepareOverviewOptions(options));
        }
};

// Response Time Percentiles Over Time
function refreshResponseTimePercentilesOverTime(fixTimestamps) {
    var infos = responseTimePercentilesOverTimeInfos;
    prepareSeries(infos.data);
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, -10800000);
    }
    if(isGraph($("#flotResponseTimePercentilesOverTime"))) {
        infos.createGraph();
    }else {
        var choiceContainer = $("#choicesResponseTimePercentilesOverTime");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotResponseTimePercentilesOverTime", "#overviewResponseTimePercentilesOverTime");
        $('#footerResponseTimePercentilesOverTime .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};


var responseTimeVsRequestInfos = {
    data: {"result": {"minY": 305.0, "minX": 7.0, "maxY": 4110.0, "series": [{"data": [[9.0, 4032.0], [10.0, 3937.0], [11.0, 3989.0], [12.0, 4110.0], [13.0, 4076.0], [7.0, 305.0], [14.0, 3955.5], [15.0, 1427.0]], "isOverall": false, "label": "Successes", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 1000, "maxX": 15.0, "title": "Response Time Vs Request"}},
    getOptions: function() {
        return {
            series: {
                lines: {
                    show: false
                },
                points: {
                    show: true
                }
            },
            xaxis: {
                axisLabel: "Global number of requests per second",
                axisLabelUseCanvas: true,
                axisLabelFontSizePixels: 12,
                axisLabelFontFamily: 'Verdana, Arial',
                axisLabelPadding: 20,
            },
            yaxis: {
                axisLabel: "Median Response Time in ms",
                axisLabelUseCanvas: true,
                axisLabelFontSizePixels: 12,
                axisLabelFontFamily: 'Verdana, Arial',
                axisLabelPadding: 20,
            },
            legend: {
                noColumns: 2,
                show: true,
                container: '#legendResponseTimeVsRequest'
            },
            selection: {
                mode: 'xy'
            },
            grid: {
                hoverable: true // IMPORTANT! this is needed for tooltip to work
            },
            tooltip: true,
            tooltipOpts: {
                content: "%s : Median response time at %x req/s was %y ms"
            },
            colors: ["#9ACD32", "#FF6347"]
        };
    },
    createGraph: function () {
        var data = this.data;
        var dataset = prepareData(data.result.series, $("#choicesResponseTimeVsRequest"));
        var options = this.getOptions();
        prepareOptions(options, data);
        $.plot($("#flotResponseTimeVsRequest"), dataset, options);
        // setup overview
        $.plot($("#overviewResponseTimeVsRequest"), dataset, prepareOverviewOptions(options));

    }
};

// Response Time vs Request
function refreshResponseTimeVsRequest() {
    var infos = responseTimeVsRequestInfos;
    prepareSeries(infos.data);
    if (isGraph($("#flotResponseTimeVsRequest"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesResponseTimeVsRequest");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotResponseTimeVsRequest", "#overviewResponseTimeVsRequest");
        $('#footerResponseRimeVsRequest .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};


var latenciesVsRequestInfos = {
    data: {"result": {"minY": 305.0, "minX": 7.0, "maxY": 4110.0, "series": [{"data": [[9.0, 4032.0], [10.0, 3937.0], [11.0, 3989.0], [12.0, 4110.0], [13.0, 4076.0], [7.0, 305.0], [14.0, 3955.5], [15.0, 1427.0]], "isOverall": false, "label": "Successes", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 1000, "maxX": 15.0, "title": "Latencies Vs Request"}},
    getOptions: function() {
        return{
            series: {
                lines: {
                    show: false
                },
                points: {
                    show: true
                }
            },
            xaxis: {
                axisLabel: "Global number of requests per second",
                axisLabelUseCanvas: true,
                axisLabelFontSizePixels: 12,
                axisLabelFontFamily: 'Verdana, Arial',
                axisLabelPadding: 20,
            },
            yaxis: {
                axisLabel: "Median Latency in ms",
                axisLabelUseCanvas: true,
                axisLabelFontSizePixels: 12,
                axisLabelFontFamily: 'Verdana, Arial',
                axisLabelPadding: 20,
            },
            legend: { noColumns: 2,show: true, container: '#legendLatencyVsRequest' },
            selection: {
                mode: 'xy'
            },
            grid: {
                hoverable: true // IMPORTANT! this is needed for tooltip to work
            },
            tooltip: true,
            tooltipOpts: {
                content: "%s : Median Latency time at %x req/s was %y ms"
            },
            colors: ["#9ACD32", "#FF6347"]
        };
    },
    createGraph: function () {
        var data = this.data;
        var dataset = prepareData(data.result.series, $("#choicesLatencyVsRequest"));
        var options = this.getOptions();
        prepareOptions(options, data);
        $.plot($("#flotLatenciesVsRequest"), dataset, options);
        // setup overview
        $.plot($("#overviewLatenciesVsRequest"), dataset, prepareOverviewOptions(options));
    }
};

// Latencies vs Request
function refreshLatenciesVsRequest() {
        var infos = latenciesVsRequestInfos;
        prepareSeries(infos.data);
        if(isGraph($("#flotLatenciesVsRequest"))){
            infos.createGraph();
        }else{
            var choiceContainer = $("#choicesLatencyVsRequest");
            createLegend(choiceContainer, infos);
            infos.createGraph();
            setGraphZoomable("#flotLatenciesVsRequest", "#overviewLatenciesVsRequest");
            $('#footerLatenciesVsRequest .legendColorBox > div').each(function(i){
                $(this).clone().prependTo(choiceContainer.find("li").eq(i));
            });
        }
};

var hitsPerSecondInfos = {
        data: {"result": {"minY": 3.0166666666666666, "minX": 1.78260024E12, "maxY": 11.933333333333334, "series": [{"data": [[1.7826003E12, 11.933333333333334], [1.78260024E12, 3.0166666666666666], [1.78260036E12, 8.966666666666667]], "isOverall": false, "label": "hitsPerSecond", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.78260036E12, "title": "Hits Per Second"}},
        getOptions: function() {
            return {
                series: {
                    lines: {
                        show: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getElapsedTimeLabel(this.data.result.granularity),
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Number of hits / sec",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: "#legendHitsPerSecond"
                },
                selection: {
                    mode : 'xy'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s at %x was %y.2 hits/sec"
                }
            };
        },
        createGraph: function createGraph() {
            var data = this.data;
            var dataset = prepareData(data.result.series, $("#choicesHitsPerSecond"));
            var options = this.getOptions();
            prepareOptions(options, data);
            $.plot($("#flotHitsPerSecond"), dataset, options);
            // setup overview
            $.plot($("#overviewHitsPerSecond"), dataset, prepareOverviewOptions(options));
        }
};

// Hits per second
function refreshHitsPerSecond(fixTimestamps) {
    var infos = hitsPerSecondInfos;
    prepareSeries(infos.data);
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, -10800000);
    }
    if (isGraph($("#flotHitsPerSecond"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesHitsPerSecond");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotHitsPerSecond", "#overviewHitsPerSecond");
        $('#footerHitsPerSecond .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
}

var codesPerSecondInfos = {
        data: {"result": {"minY": 2.45, "minX": 1.78260024E12, "maxY": 11.666666666666666, "series": [{"data": [[1.7826003E12, 11.666666666666666], [1.78260024E12, 2.45], [1.78260036E12, 9.8]], "isOverall": false, "label": "200", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.78260036E12, "title": "Codes Per Second"}},
        getOptions: function(){
            return {
                series: {
                    lines: {
                        show: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getElapsedTimeLabel(this.data.result.granularity),
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Number of responses / sec",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: "#legendCodesPerSecond"
                },
                selection: {
                    mode: 'xy'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "Number of Response Codes %s at %x was %y.2 responses / sec"
                }
            };
        },
    createGraph: function() {
        var data = this.data;
        var dataset = prepareData(data.result.series, $("#choicesCodesPerSecond"));
        var options = this.getOptions();
        prepareOptions(options, data);
        $.plot($("#flotCodesPerSecond"), dataset, options);
        // setup overview
        $.plot($("#overviewCodesPerSecond"), dataset, prepareOverviewOptions(options));
    }
};

// Codes per second
function refreshCodesPerSecond(fixTimestamps) {
    var infos = codesPerSecondInfos;
    prepareSeries(infos.data);
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, -10800000);
    }
    if(isGraph($("#flotCodesPerSecond"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesCodesPerSecond");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotCodesPerSecond", "#overviewCodesPerSecond");
        $('#footerCodesPerSecond .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};

var transactionsPerSecondInfos = {
        data: {"result": {"minY": 2.45, "minX": 1.78260024E12, "maxY": 11.666666666666666, "series": [{"data": [[1.7826003E12, 11.666666666666666], [1.78260024E12, 2.45], [1.78260036E12, 9.8]], "isOverall": false, "label": "GET /products by name-success", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.78260036E12, "title": "Transactions Per Second"}},
        getOptions: function(){
            return {
                series: {
                    lines: {
                        show: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getElapsedTimeLabel(this.data.result.granularity),
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Number of transactions / sec",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: "#legendTransactionsPerSecond"
                },
                selection: {
                    mode: 'xy'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s at %x was %y transactions / sec"
                }
            };
        },
    createGraph: function () {
        var data = this.data;
        var dataset = prepareData(data.result.series, $("#choicesTransactionsPerSecond"));
        var options = this.getOptions();
        prepareOptions(options, data);
        $.plot($("#flotTransactionsPerSecond"), dataset, options);
        // setup overview
        $.plot($("#overviewTransactionsPerSecond"), dataset, prepareOverviewOptions(options));
    }
};

// Transactions per second
function refreshTransactionsPerSecond(fixTimestamps) {
    var infos = transactionsPerSecondInfos;
    prepareSeries(infos.data);
    if(infos.data.result.series.length == 0) {
        setEmptyGraph("#bodyTransactionsPerSecond");
        return;
    }
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, -10800000);
    }
    if(isGraph($("#flotTransactionsPerSecond"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesTransactionsPerSecond");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotTransactionsPerSecond", "#overviewTransactionsPerSecond");
        $('#footerTransactionsPerSecond .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};

var totalTPSInfos = {
        data: {"result": {"minY": 2.45, "minX": 1.78260024E12, "maxY": 11.666666666666666, "series": [{"data": [[1.7826003E12, 11.666666666666666], [1.78260024E12, 2.45], [1.78260036E12, 9.8]], "isOverall": false, "label": "Transaction-success", "isController": false}, {"data": [], "isOverall": false, "label": "Transaction-failure", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.78260036E12, "title": "Total Transactions Per Second"}},
        getOptions: function(){
            return {
                series: {
                    lines: {
                        show: true
                    },
                    points: {
                        show: true
                    }
                },
                xaxis: {
                    mode: "time",
                    timeformat: getTimeFormat(this.data.result.granularity),
                    axisLabel: getElapsedTimeLabel(this.data.result.granularity),
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20,
                },
                yaxis: {
                    axisLabel: "Number of transactions / sec",
                    axisLabelUseCanvas: true,
                    axisLabelFontSizePixels: 12,
                    axisLabelFontFamily: 'Verdana, Arial',
                    axisLabelPadding: 20
                },
                legend: {
                    noColumns: 2,
                    show: true,
                    container: "#legendTotalTPS"
                },
                selection: {
                    mode: 'xy'
                },
                grid: {
                    hoverable: true // IMPORTANT! this is needed for tooltip to
                                    // work
                },
                tooltip: true,
                tooltipOpts: {
                    content: "%s at %x was %y transactions / sec"
                },
                colors: ["#9ACD32", "#FF6347"]
            };
        },
    createGraph: function () {
        var data = this.data;
        var dataset = prepareData(data.result.series, $("#choicesTotalTPS"));
        var options = this.getOptions();
        prepareOptions(options, data);
        $.plot($("#flotTotalTPS"), dataset, options);
        // setup overview
        $.plot($("#overviewTotalTPS"), dataset, prepareOverviewOptions(options));
    }
};

// Total Transactions per second
function refreshTotalTPS(fixTimestamps) {
    var infos = totalTPSInfos;
    // We want to ignore seriesFilter
    prepareSeries(infos.data, false, true);
    if(fixTimestamps) {
        fixTimeStamps(infos.data.result.series, -10800000);
    }
    if(isGraph($("#flotTotalTPS"))){
        infos.createGraph();
    }else{
        var choiceContainer = $("#choicesTotalTPS");
        createLegend(choiceContainer, infos);
        infos.createGraph();
        setGraphZoomable("#flotTotalTPS", "#overviewTotalTPS");
        $('#footerTotalTPS .legendColorBox > div').each(function(i){
            $(this).clone().prependTo(choiceContainer.find("li").eq(i));
        });
    }
};

// Collapse the graph matching the specified DOM element depending the collapsed
// status
function collapse(elem, collapsed){
    if(collapsed){
        $(elem).parent().find(".fa-chevron-up").removeClass("fa-chevron-up").addClass("fa-chevron-down");
    } else {
        $(elem).parent().find(".fa-chevron-down").removeClass("fa-chevron-down").addClass("fa-chevron-up");
        if (elem.id == "bodyBytesThroughputOverTime") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshBytesThroughputOverTime(true);
            }
            document.location.href="#bytesThroughputOverTime";
        } else if (elem.id == "bodyLatenciesOverTime") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshLatenciesOverTime(true);
            }
            document.location.href="#latenciesOverTime";
        } else if (elem.id == "bodyCustomGraph") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshCustomGraph(true);
            }
            document.location.href="#responseCustomGraph";
        } else if (elem.id == "bodyConnectTimeOverTime") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshConnectTimeOverTime(true);
            }
            document.location.href="#connectTimeOverTime";
        } else if (elem.id == "bodyResponseTimePercentilesOverTime") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshResponseTimePercentilesOverTime(true);
            }
            document.location.href="#responseTimePercentilesOverTime";
        } else if (elem.id == "bodyResponseTimeDistribution") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshResponseTimeDistribution();
            }
            document.location.href="#responseTimeDistribution" ;
        } else if (elem.id == "bodySyntheticResponseTimeDistribution") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshSyntheticResponseTimeDistribution();
            }
            document.location.href="#syntheticResponseTimeDistribution" ;
        } else if (elem.id == "bodyActiveThreadsOverTime") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshActiveThreadsOverTime(true);
            }
            document.location.href="#activeThreadsOverTime";
        } else if (elem.id == "bodyTimeVsThreads") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshTimeVsThreads();
            }
            document.location.href="#timeVsThreads" ;
        } else if (elem.id == "bodyCodesPerSecond") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshCodesPerSecond(true);
            }
            document.location.href="#codesPerSecond";
        } else if (elem.id == "bodyTransactionsPerSecond") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshTransactionsPerSecond(true);
            }
            document.location.href="#transactionsPerSecond";
        } else if (elem.id == "bodyTotalTPS") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshTotalTPS(true);
            }
            document.location.href="#totalTPS";
        } else if (elem.id == "bodyResponseTimeVsRequest") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshResponseTimeVsRequest();
            }
            document.location.href="#responseTimeVsRequest";
        } else if (elem.id == "bodyLatenciesVsRequest") {
            if (isGraph($(elem).find('.flot-chart-content')) == false) {
                refreshLatenciesVsRequest();
            }
            document.location.href="#latencyVsRequest";
        }
    }
}

/*
 * Activates or deactivates all series of the specified graph (represented by id parameter)
 * depending on checked argument.
 */
function toggleAll(id, checked){
    var placeholder = document.getElementById(id);

    var cases = $(placeholder).find(':checkbox');
    cases.prop('checked', checked);
    $(cases).parent().children().children().toggleClass("legend-disabled", !checked);

    var choiceContainer;
    if ( id == "choicesBytesThroughputOverTime"){
        choiceContainer = $("#choicesBytesThroughputOverTime");
        refreshBytesThroughputOverTime(false);
    } else if(id == "choicesResponseTimesOverTime"){
        choiceContainer = $("#choicesResponseTimesOverTime");
        refreshResponseTimeOverTime(false);
    }else if(id == "choicesResponseCustomGraph"){
        choiceContainer = $("#choicesResponseCustomGraph");
        refreshCustomGraph(false);
    } else if ( id == "choicesLatenciesOverTime"){
        choiceContainer = $("#choicesLatenciesOverTime");
        refreshLatenciesOverTime(false);
    } else if ( id == "choicesConnectTimeOverTime"){
        choiceContainer = $("#choicesConnectTimeOverTime");
        refreshConnectTimeOverTime(false);
    } else if ( id == "choicesResponseTimePercentilesOverTime"){
        choiceContainer = $("#choicesResponseTimePercentilesOverTime");
        refreshResponseTimePercentilesOverTime(false);
    } else if ( id == "choicesResponseTimePercentiles"){
        choiceContainer = $("#choicesResponseTimePercentiles");
        refreshResponseTimePercentiles();
    } else if(id == "choicesActiveThreadsOverTime"){
        choiceContainer = $("#choicesActiveThreadsOverTime");
        refreshActiveThreadsOverTime(false);
    } else if ( id == "choicesTimeVsThreads"){
        choiceContainer = $("#choicesTimeVsThreads");
        refreshTimeVsThreads();
    } else if ( id == "choicesSyntheticResponseTimeDistribution"){
        choiceContainer = $("#choicesSyntheticResponseTimeDistribution");
        refreshSyntheticResponseTimeDistribution();
    } else if ( id == "choicesResponseTimeDistribution"){
        choiceContainer = $("#choicesResponseTimeDistribution");
        refreshResponseTimeDistribution();
    } else if ( id == "choicesHitsPerSecond"){
        choiceContainer = $("#choicesHitsPerSecond");
        refreshHitsPerSecond(false);
    } else if(id == "choicesCodesPerSecond"){
        choiceContainer = $("#choicesCodesPerSecond");
        refreshCodesPerSecond(false);
    } else if ( id == "choicesTransactionsPerSecond"){
        choiceContainer = $("#choicesTransactionsPerSecond");
        refreshTransactionsPerSecond(false);
    } else if ( id == "choicesTotalTPS"){
        choiceContainer = $("#choicesTotalTPS");
        refreshTotalTPS(false);
    } else if ( id == "choicesResponseTimeVsRequest"){
        choiceContainer = $("#choicesResponseTimeVsRequest");
        refreshResponseTimeVsRequest();
    } else if ( id == "choicesLatencyVsRequest"){
        choiceContainer = $("#choicesLatencyVsRequest");
        refreshLatenciesVsRequest();
    }
    var color = checked ? "black" : "#818181";
    if(choiceContainer != null) {
        choiceContainer.find("label").each(function(){
            this.style.color = color;
        });
    }
}

