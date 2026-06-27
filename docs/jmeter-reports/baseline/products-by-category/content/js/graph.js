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
        data: {"result": {"minY": 507.0, "minX": 0.0, "maxY": 8002.0, "series": [{"data": [[0.0, 507.0], [0.1, 508.0], [0.2, 571.0], [0.3, 701.0], [0.4, 741.0], [0.5, 783.0], [0.6, 795.0], [0.7, 865.0], [0.8, 868.0], [0.9, 880.0], [1.0, 883.0], [1.1, 915.0], [1.2, 971.0], [1.3, 975.0], [1.4, 1014.0], [1.5, 1034.0], [1.6, 1096.0], [1.7, 1098.0], [1.8, 1135.0], [1.9, 1182.0], [2.0, 1238.0], [2.1, 1249.0], [2.2, 1263.0], [2.3, 1264.0], [2.4, 1268.0], [2.5, 1293.0], [2.6, 1312.0], [2.7, 1327.0], [2.8, 1338.0], [2.9, 1346.0], [3.0, 1352.0], [3.1, 1368.0], [3.2, 1369.0], [3.3, 1382.0], [3.4, 1410.0], [3.5, 1428.0], [3.6, 1550.0], [3.7, 1559.0], [3.8, 1564.0], [3.9, 1574.0], [4.0, 1589.0], [4.1, 1627.0], [4.2, 1659.0], [4.3, 1684.0], [4.4, 1706.0], [4.5, 1734.0], [4.6, 1752.0], [4.7, 1763.0], [4.8, 1771.0], [4.9, 1780.0], [5.0, 1802.0], [5.1, 1814.0], [5.2, 1814.0], [5.3, 1823.0], [5.4, 1830.0], [5.5, 1838.0], [5.6, 1850.0], [5.7, 1963.0], [5.8, 1964.0], [5.9, 1964.0], [6.0, 1965.0], [6.1, 1983.0], [6.2, 2009.0], [6.3, 2010.0], [6.4, 2015.0], [6.5, 2022.0], [6.6, 2067.0], [6.7, 2069.0], [6.8, 2078.0], [6.9, 2086.0], [7.0, 2093.0], [7.1, 2150.0], [7.2, 2153.0], [7.3, 2164.0], [7.4, 2166.0], [7.5, 2195.0], [7.6, 2252.0], [7.7, 2275.0], [7.8, 2286.0], [7.9, 2304.0], [8.0, 2305.0], [8.1, 2315.0], [8.2, 2360.0], [8.3, 2367.0], [8.4, 2435.0], [8.5, 2439.0], [8.6, 2479.0], [8.7, 2485.0], [8.8, 2566.0], [8.9, 2572.0], [9.0, 2629.0], [9.1, 2642.0], [9.2, 2677.0], [9.3, 2677.0], [9.4, 2709.0], [9.5, 2734.0], [9.6, 2737.0], [9.7, 2778.0], [9.8, 2778.0], [9.9, 2819.0], [10.0, 2830.0], [10.1, 2841.0], [10.2, 2853.0], [10.3, 2906.0], [10.4, 2917.0], [10.5, 2920.0], [10.6, 2941.0], [10.7, 2969.0], [10.8, 2987.0], [10.9, 2999.0], [11.0, 3006.0], [11.1, 3030.0], [11.2, 3041.0], [11.3, 3072.0], [11.4, 3084.0], [11.5, 3124.0], [11.6, 3161.0], [11.7, 3167.0], [11.8, 3167.0], [11.9, 3198.0], [12.0, 3199.0], [12.1, 3205.0], [12.2, 3206.0], [12.3, 3222.0], [12.4, 3223.0], [12.5, 3234.0], [12.6, 3234.0], [12.7, 3245.0], [12.8, 3247.0], [12.9, 3250.0], [13.0, 3261.0], [13.1, 3266.0], [13.2, 3274.0], [13.3, 3277.0], [13.4, 3284.0], [13.5, 3286.0], [13.6, 3293.0], [13.7, 3302.0], [13.8, 3307.0], [13.9, 3308.0], [14.0, 3310.0], [14.1, 3326.0], [14.2, 3328.0], [14.3, 3346.0], [14.4, 3354.0], [14.5, 3365.0], [14.6, 3366.0], [14.7, 3371.0], [14.8, 3372.0], [14.9, 3375.0], [15.0, 3379.0], [15.1, 3385.0], [15.2, 3392.0], [15.3, 3396.0], [15.4, 3402.0], [15.5, 3406.0], [15.6, 3411.0], [15.7, 3422.0], [15.8, 3430.0], [15.9, 3435.0], [16.0, 3439.0], [16.1, 3439.0], [16.2, 3450.0], [16.3, 3455.0], [16.4, 3460.0], [16.5, 3469.0], [16.6, 3469.0], [16.7, 3470.0], [16.8, 3475.0], [16.9, 3481.0], [17.0, 3481.0], [17.1, 3487.0], [17.2, 3487.0], [17.3, 3489.0], [17.4, 3491.0], [17.5, 3493.0], [17.6, 3496.0], [17.7, 3498.0], [17.8, 3502.0], [17.9, 3504.0], [18.0, 3508.0], [18.1, 3510.0], [18.2, 3516.0], [18.3, 3518.0], [18.4, 3528.0], [18.5, 3533.0], [18.6, 3538.0], [18.7, 3538.0], [18.8, 3541.0], [18.9, 3544.0], [19.0, 3544.0], [19.1, 3545.0], [19.2, 3547.0], [19.3, 3550.0], [19.4, 3553.0], [19.5, 3555.0], [19.6, 3559.0], [19.7, 3562.0], [19.8, 3564.0], [19.9, 3565.0], [20.0, 3570.0], [20.1, 3574.0], [20.2, 3576.0], [20.3, 3577.0], [20.4, 3582.0], [20.5, 3583.0], [20.6, 3584.0], [20.7, 3585.0], [20.8, 3587.0], [20.9, 3588.0], [21.0, 3588.0], [21.1, 3591.0], [21.2, 3592.0], [21.3, 3594.0], [21.4, 3598.0], [21.5, 3603.0], [21.6, 3605.0], [21.7, 3607.0], [21.8, 3607.0], [21.9, 3608.0], [22.0, 3608.0], [22.1, 3609.0], [22.2, 3609.0], [22.3, 3610.0], [22.4, 3615.0], [22.5, 3616.0], [22.6, 3619.0], [22.7, 3619.0], [22.8, 3622.0], [22.9, 3623.0], [23.0, 3625.0], [23.1, 3625.0], [23.2, 3630.0], [23.3, 3633.0], [23.4, 3634.0], [23.5, 3635.0], [23.6, 3636.0], [23.7, 3638.0], [23.8, 3641.0], [23.9, 3643.0], [24.0, 3643.0], [24.1, 3647.0], [24.2, 3648.0], [24.3, 3649.0], [24.4, 3651.0], [24.5, 3652.0], [24.6, 3653.0], [24.7, 3653.0], [24.8, 3656.0], [24.9, 3656.0], [25.0, 3659.0], [25.1, 3659.0], [25.2, 3662.0], [25.3, 3662.0], [25.4, 3663.0], [25.5, 3665.0], [25.6, 3666.0], [25.7, 3667.0], [25.8, 3668.0], [25.9, 3668.0], [26.0, 3669.0], [26.1, 3673.0], [26.2, 3674.0], [26.3, 3677.0], [26.4, 3677.0], [26.5, 3677.0], [26.6, 3677.0], [26.7, 3679.0], [26.8, 3679.0], [26.9, 3680.0], [27.0, 3682.0], [27.1, 3682.0], [27.2, 3685.0], [27.3, 3685.0], [27.4, 3687.0], [27.5, 3687.0], [27.6, 3689.0], [27.7, 3690.0], [27.8, 3692.0], [27.9, 3693.0], [28.0, 3693.0], [28.1, 3694.0], [28.2, 3694.0], [28.3, 3696.0], [28.4, 3698.0], [28.5, 3698.0], [28.6, 3700.0], [28.7, 3701.0], [28.8, 3701.0], [28.9, 3703.0], [29.0, 3704.0], [29.1, 3704.0], [29.2, 3706.0], [29.3, 3706.0], [29.4, 3707.0], [29.5, 3707.0], [29.6, 3708.0], [29.7, 3708.0], [29.8, 3710.0], [29.9, 3710.0], [30.0, 3711.0], [30.1, 3712.0], [30.2, 3714.0], [30.3, 3714.0], [30.4, 3714.0], [30.5, 3715.0], [30.6, 3715.0], [30.7, 3717.0], [30.8, 3717.0], [30.9, 3718.0], [31.0, 3718.0], [31.1, 3721.0], [31.2, 3723.0], [31.3, 3725.0], [31.4, 3727.0], [31.5, 3729.0], [31.6, 3731.0], [31.7, 3732.0], [31.8, 3732.0], [31.9, 3733.0], [32.0, 3735.0], [32.1, 3735.0], [32.2, 3737.0], [32.3, 3737.0], [32.4, 3739.0], [32.5, 3739.0], [32.6, 3739.0], [32.7, 3741.0], [32.8, 3741.0], [32.9, 3742.0], [33.0, 3744.0], [33.1, 3745.0], [33.2, 3746.0], [33.3, 3746.0], [33.4, 3747.0], [33.5, 3748.0], [33.6, 3749.0], [33.7, 3749.0], [33.8, 3750.0], [33.9, 3753.0], [34.0, 3755.0], [34.1, 3755.0], [34.2, 3758.0], [34.3, 3758.0], [34.4, 3759.0], [34.5, 3761.0], [34.6, 3761.0], [34.7, 3764.0], [34.8, 3764.0], [34.9, 3764.0], [35.0, 3765.0], [35.1, 3765.0], [35.2, 3766.0], [35.3, 3766.0], [35.4, 3766.0], [35.5, 3767.0], [35.6, 3767.0], [35.7, 3768.0], [35.8, 3768.0], [35.9, 3772.0], [36.0, 3772.0], [36.1, 3772.0], [36.2, 3776.0], [36.3, 3777.0], [36.4, 3779.0], [36.5, 3780.0], [36.6, 3783.0], [36.7, 3784.0], [36.8, 3785.0], [36.9, 3785.0], [37.0, 3787.0], [37.1, 3787.0], [37.2, 3788.0], [37.3, 3788.0], [37.4, 3790.0], [37.5, 3791.0], [37.6, 3792.0], [37.7, 3792.0], [37.8, 3792.0], [37.9, 3793.0], [38.0, 3793.0], [38.1, 3794.0], [38.2, 3795.0], [38.3, 3796.0], [38.4, 3800.0], [38.5, 3803.0], [38.6, 3805.0], [38.7, 3806.0], [38.8, 3807.0], [38.9, 3808.0], [39.0, 3808.0], [39.1, 3808.0], [39.2, 3810.0], [39.3, 3811.0], [39.4, 3813.0], [39.5, 3814.0], [39.6, 3814.0], [39.7, 3814.0], [39.8, 3815.0], [39.9, 3816.0], [40.0, 3817.0], [40.1, 3817.0], [40.2, 3818.0], [40.3, 3818.0], [40.4, 3819.0], [40.5, 3823.0], [40.6, 3826.0], [40.7, 3826.0], [40.8, 3827.0], [40.9, 3827.0], [41.0, 3827.0], [41.1, 3828.0], [41.2, 3828.0], [41.3, 3832.0], [41.4, 3834.0], [41.5, 3835.0], [41.6, 3837.0], [41.7, 3837.0], [41.8, 3838.0], [41.9, 3840.0], [42.0, 3841.0], [42.1, 3842.0], [42.2, 3842.0], [42.3, 3844.0], [42.4, 3845.0], [42.5, 3846.0], [42.6, 3847.0], [42.7, 3847.0], [42.8, 3849.0], [42.9, 3850.0], [43.0, 3852.0], [43.1, 3852.0], [43.2, 3853.0], [43.3, 3854.0], [43.4, 3855.0], [43.5, 3855.0], [43.6, 3857.0], [43.7, 3857.0], [43.8, 3859.0], [43.9, 3860.0], [44.0, 3860.0], [44.1, 3861.0], [44.2, 3861.0], [44.3, 3861.0], [44.4, 3862.0], [44.5, 3862.0], [44.6, 3862.0], [44.7, 3863.0], [44.8, 3864.0], [44.9, 3866.0], [45.0, 3867.0], [45.1, 3870.0], [45.2, 3871.0], [45.3, 3872.0], [45.4, 3872.0], [45.5, 3872.0], [45.6, 3873.0], [45.7, 3876.0], [45.8, 3877.0], [45.9, 3878.0], [46.0, 3884.0], [46.1, 3886.0], [46.2, 3887.0], [46.3, 3887.0], [46.4, 3888.0], [46.5, 3888.0], [46.6, 3889.0], [46.7, 3890.0], [46.8, 3890.0], [46.9, 3892.0], [47.0, 3892.0], [47.1, 3896.0], [47.2, 3896.0], [47.3, 3897.0], [47.4, 3898.0], [47.5, 3900.0], [47.6, 3903.0], [47.7, 3903.0], [47.8, 3908.0], [47.9, 3908.0], [48.0, 3909.0], [48.1, 3909.0], [48.2, 3911.0], [48.3, 3911.0], [48.4, 3913.0], [48.5, 3914.0], [48.6, 3915.0], [48.7, 3916.0], [48.8, 3916.0], [48.9, 3917.0], [49.0, 3918.0], [49.1, 3920.0], [49.2, 3921.0], [49.3, 3922.0], [49.4, 3922.0], [49.5, 3925.0], [49.6, 3926.0], [49.7, 3926.0], [49.8, 3926.0], [49.9, 3929.0], [50.0, 3929.0], [50.1, 3930.0], [50.2, 3932.0], [50.3, 3932.0], [50.4, 3933.0], [50.5, 3934.0], [50.6, 3937.0], [50.7, 3937.0], [50.8, 3938.0], [50.9, 3938.0], [51.0, 3939.0], [51.1, 3941.0], [51.2, 3945.0], [51.3, 3951.0], [51.4, 3951.0], [51.5, 3952.0], [51.6, 3952.0], [51.7, 3952.0], [51.8, 3955.0], [51.9, 3958.0], [52.0, 3958.0], [52.1, 3959.0], [52.2, 3960.0], [52.3, 3961.0], [52.4, 3964.0], [52.5, 3965.0], [52.6, 3966.0], [52.7, 3967.0], [52.8, 3969.0], [52.9, 3970.0], [53.0, 3972.0], [53.1, 3973.0], [53.2, 3975.0], [53.3, 3976.0], [53.4, 3979.0], [53.5, 3980.0], [53.6, 3982.0], [53.7, 3982.0], [53.8, 3983.0], [53.9, 3987.0], [54.0, 3991.0], [54.1, 3992.0], [54.2, 3995.0], [54.3, 3996.0], [54.4, 3996.0], [54.5, 3997.0], [54.6, 4002.0], [54.7, 4005.0], [54.8, 4005.0], [54.9, 4006.0], [55.0, 4009.0], [55.1, 4010.0], [55.2, 4013.0], [55.3, 4016.0], [55.4, 4017.0], [55.5, 4017.0], [55.6, 4021.0], [55.7, 4021.0], [55.8, 4023.0], [55.9, 4024.0], [56.0, 4025.0], [56.1, 4027.0], [56.2, 4028.0], [56.3, 4031.0], [56.4, 4031.0], [56.5, 4034.0], [56.6, 4035.0], [56.7, 4035.0], [56.8, 4035.0], [56.9, 4037.0], [57.0, 4039.0], [57.1, 4043.0], [57.2, 4045.0], [57.3, 4045.0], [57.4, 4046.0], [57.5, 4046.0], [57.6, 4048.0], [57.7, 4048.0], [57.8, 4050.0], [57.9, 4052.0], [58.0, 4055.0], [58.1, 4055.0], [58.2, 4055.0], [58.3, 4057.0], [58.4, 4059.0], [58.5, 4060.0], [58.6, 4061.0], [58.7, 4062.0], [58.8, 4066.0], [58.9, 4068.0], [59.0, 4068.0], [59.1, 4070.0], [59.2, 4071.0], [59.3, 4073.0], [59.4, 4073.0], [59.5, 4074.0], [59.6, 4075.0], [59.7, 4077.0], [59.8, 4077.0], [59.9, 4079.0], [60.0, 4080.0], [60.1, 4080.0], [60.2, 4082.0], [60.3, 4082.0], [60.4, 4086.0], [60.5, 4086.0], [60.6, 4086.0], [60.7, 4088.0], [60.8, 4092.0], [60.9, 4094.0], [61.0, 4094.0], [61.1, 4096.0], [61.2, 4100.0], [61.3, 4102.0], [61.4, 4103.0], [61.5, 4106.0], [61.6, 4106.0], [61.7, 4107.0], [61.8, 4109.0], [61.9, 4111.0], [62.0, 4113.0], [62.1, 4115.0], [62.2, 4118.0], [62.3, 4118.0], [62.4, 4119.0], [62.5, 4121.0], [62.6, 4121.0], [62.7, 4122.0], [62.8, 4122.0], [62.9, 4123.0], [63.0, 4124.0], [63.1, 4132.0], [63.2, 4132.0], [63.3, 4140.0], [63.4, 4142.0], [63.5, 4144.0], [63.6, 4145.0], [63.7, 4148.0], [63.8, 4149.0], [63.9, 4156.0], [64.0, 4157.0], [64.1, 4158.0], [64.2, 4168.0], [64.3, 4168.0], [64.4, 4169.0], [64.5, 4169.0], [64.6, 4173.0], [64.7, 4173.0], [64.8, 4175.0], [64.9, 4175.0], [65.0, 4177.0], [65.1, 4177.0], [65.2, 4182.0], [65.3, 4183.0], [65.4, 4184.0], [65.5, 4186.0], [65.6, 4188.0], [65.7, 4189.0], [65.8, 4189.0], [65.9, 4194.0], [66.0, 4197.0], [66.1, 4201.0], [66.2, 4201.0], [66.3, 4204.0], [66.4, 4205.0], [66.5, 4208.0], [66.6, 4209.0], [66.7, 4211.0], [66.8, 4213.0], [66.9, 4215.0], [67.0, 4221.0], [67.1, 4225.0], [67.2, 4227.0], [67.3, 4231.0], [67.4, 4231.0], [67.5, 4232.0], [67.6, 4233.0], [67.7, 4240.0], [67.8, 4244.0], [67.9, 4245.0], [68.0, 4250.0], [68.1, 4251.0], [68.2, 4251.0], [68.3, 4252.0], [68.4, 4254.0], [68.5, 4263.0], [68.6, 4269.0], [68.7, 4271.0], [68.8, 4275.0], [68.9, 4275.0], [69.0, 4281.0], [69.1, 4284.0], [69.2, 4291.0], [69.3, 4294.0], [69.4, 4294.0], [69.5, 4295.0], [69.6, 4298.0], [69.7, 4299.0], [69.8, 4299.0], [69.9, 4308.0], [70.0, 4310.0], [70.1, 4313.0], [70.2, 4314.0], [70.3, 4315.0], [70.4, 4320.0], [70.5, 4328.0], [70.6, 4331.0], [70.7, 4333.0], [70.8, 4335.0], [70.9, 4337.0], [71.0, 4339.0], [71.1, 4340.0], [71.2, 4343.0], [71.3, 4349.0], [71.4, 4352.0], [71.5, 4353.0], [71.6, 4355.0], [71.7, 4355.0], [71.8, 4356.0], [71.9, 4356.0], [72.0, 4366.0], [72.1, 4366.0], [72.2, 4368.0], [72.3, 4368.0], [72.4, 4369.0], [72.5, 4370.0], [72.6, 4370.0], [72.7, 4376.0], [72.8, 4376.0], [72.9, 4378.0], [73.0, 4378.0], [73.1, 4381.0], [73.2, 4383.0], [73.3, 4383.0], [73.4, 4384.0], [73.5, 4384.0], [73.6, 4387.0], [73.7, 4387.0], [73.8, 4393.0], [73.9, 4397.0], [74.0, 4397.0], [74.1, 4398.0], [74.2, 4399.0], [74.3, 4400.0], [74.4, 4405.0], [74.5, 4407.0], [74.6, 4408.0], [74.7, 4417.0], [74.8, 4417.0], [74.9, 4419.0], [75.0, 4420.0], [75.1, 4421.0], [75.2, 4421.0], [75.3, 4423.0], [75.4, 4424.0], [75.5, 4430.0], [75.6, 4431.0], [75.7, 4432.0], [75.8, 4433.0], [75.9, 4434.0], [76.0, 4436.0], [76.1, 4436.0], [76.2, 4439.0], [76.3, 4442.0], [76.4, 4450.0], [76.5, 4453.0], [76.6, 4455.0], [76.7, 4457.0], [76.8, 4459.0], [76.9, 4460.0], [77.0, 4460.0], [77.1, 4464.0], [77.2, 4465.0], [77.3, 4468.0], [77.4, 4471.0], [77.5, 4473.0], [77.6, 4474.0], [77.7, 4476.0], [77.8, 4479.0], [77.9, 4486.0], [78.0, 4488.0], [78.1, 4489.0], [78.2, 4494.0], [78.3, 4497.0], [78.4, 4499.0], [78.5, 4503.0], [78.6, 4503.0], [78.7, 4504.0], [78.8, 4505.0], [78.9, 4505.0], [79.0, 4508.0], [79.1, 4508.0], [79.2, 4510.0], [79.3, 4511.0], [79.4, 4513.0], [79.5, 4514.0], [79.6, 4517.0], [79.7, 4520.0], [79.8, 4521.0], [79.9, 4521.0], [80.0, 4522.0], [80.1, 4523.0], [80.2, 4523.0], [80.3, 4524.0], [80.4, 4526.0], [80.5, 4526.0], [80.6, 4529.0], [80.7, 4529.0], [80.8, 4533.0], [80.9, 4533.0], [81.0, 4537.0], [81.1, 4537.0], [81.2, 4538.0], [81.3, 4541.0], [81.4, 4541.0], [81.5, 4543.0], [81.6, 4545.0], [81.7, 4547.0], [81.8, 4548.0], [81.9, 4548.0], [82.0, 4548.0], [82.1, 4551.0], [82.2, 4553.0], [82.3, 4557.0], [82.4, 4558.0], [82.5, 4560.0], [82.6, 4560.0], [82.7, 4560.0], [82.8, 4564.0], [82.9, 4566.0], [83.0, 4568.0], [83.1, 4569.0], [83.2, 4570.0], [83.3, 4572.0], [83.4, 4579.0], [83.5, 4579.0], [83.6, 4580.0], [83.7, 4581.0], [83.8, 4584.0], [83.9, 4586.0], [84.0, 4586.0], [84.1, 4588.0], [84.2, 4589.0], [84.3, 4591.0], [84.4, 4594.0], [84.5, 4596.0], [84.6, 4596.0], [84.7, 4596.0], [84.8, 4597.0], [84.9, 4598.0], [85.0, 4599.0], [85.1, 4602.0], [85.2, 4605.0], [85.3, 4605.0], [85.4, 4609.0], [85.5, 4609.0], [85.6, 4612.0], [85.7, 4613.0], [85.8, 4615.0], [85.9, 4618.0], [86.0, 4619.0], [86.1, 4619.0], [86.2, 4622.0], [86.3, 4627.0], [86.4, 4627.0], [86.5, 4628.0], [86.6, 4629.0], [86.7, 4631.0], [86.8, 4632.0], [86.9, 4643.0], [87.0, 4644.0], [87.1, 4647.0], [87.2, 4648.0], [87.3, 4648.0], [87.4, 4651.0], [87.5, 4652.0], [87.6, 4654.0], [87.7, 4655.0], [87.8, 4659.0], [87.9, 4659.0], [88.0, 4662.0], [88.1, 4663.0], [88.2, 4665.0], [88.3, 4667.0], [88.4, 4668.0], [88.5, 4671.0], [88.6, 4671.0], [88.7, 4672.0], [88.8, 4674.0], [88.9, 4676.0], [89.0, 4677.0], [89.1, 4682.0], [89.2, 4682.0], [89.3, 4686.0], [89.4, 4691.0], [89.5, 4696.0], [89.6, 4697.0], [89.7, 4698.0], [89.8, 4703.0], [89.9, 4706.0], [90.0, 4709.0], [90.1, 4713.0], [90.2, 4715.0], [90.3, 4716.0], [90.4, 4720.0], [90.5, 4721.0], [90.6, 4726.0], [90.7, 4729.0], [90.8, 4729.0], [90.9, 4730.0], [91.0, 4733.0], [91.1, 4735.0], [91.2, 4736.0], [91.3, 4737.0], [91.4, 4739.0], [91.5, 4744.0], [91.6, 4748.0], [91.7, 4753.0], [91.8, 4754.0], [91.9, 4754.0], [92.0, 4758.0], [92.1, 4759.0], [92.2, 4766.0], [92.3, 4776.0], [92.4, 4778.0], [92.5, 4779.0], [92.6, 4781.0], [92.7, 4783.0], [92.8, 4785.0], [92.9, 4785.0], [93.0, 4787.0], [93.1, 4787.0], [93.2, 4790.0], [93.3, 4793.0], [93.4, 4794.0], [93.5, 4801.0], [93.6, 4802.0], [93.7, 4808.0], [93.8, 4810.0], [93.9, 4813.0], [94.0, 4817.0], [94.1, 4819.0], [94.2, 4821.0], [94.3, 4823.0], [94.4, 4823.0], [94.5, 4828.0], [94.6, 4832.0], [94.7, 4832.0], [94.8, 4836.0], [94.9, 4841.0], [95.0, 4844.0], [95.1, 4845.0], [95.2, 4850.0], [95.3, 4854.0], [95.4, 4856.0], [95.5, 4857.0], [95.6, 4858.0], [95.7, 4860.0], [95.8, 4861.0], [95.9, 4871.0], [96.0, 4872.0], [96.1, 4877.0], [96.2, 4882.0], [96.3, 4886.0], [96.4, 4888.0], [96.5, 4892.0], [96.6, 4895.0], [96.7, 4902.0], [96.8, 4909.0], [96.9, 4909.0], [97.0, 4915.0], [97.1, 4927.0], [97.2, 4936.0], [97.3, 4942.0], [97.4, 4967.0], [97.5, 4968.0], [97.6, 4971.0], [97.7, 4971.0], [97.8, 4978.0], [97.9, 5002.0], [98.0, 5006.0], [98.1, 5019.0], [98.2, 5021.0], [98.3, 5037.0], [98.4, 5054.0], [98.5, 5058.0], [98.6, 5062.0], [98.7, 5063.0], [98.8, 5075.0], [98.9, 5076.0], [99.0, 5087.0], [99.1, 5124.0], [99.2, 5157.0], [99.3, 5168.0], [99.4, 5197.0], [99.5, 5207.0], [99.6, 5252.0], [99.7, 5416.0], [99.8, 6816.0], [99.9, 7143.0], [100.0, 8002.0]], "isOverall": false, "label": "GET /products by category", "isController": false}], "supportsControllersDiscrimination": true, "maxX": 100.0, "title": "Response Time Percentiles"}},
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
        data: {"result": {"minY": 1.0, "minX": 500.0, "maxY": 143.0, "series": [{"data": [[700.0, 6.0], [800.0, 7.0], [900.0, 4.0], [1000.0, 5.0], [1100.0, 3.0], [1200.0, 9.0], [1300.0, 12.0], [1400.0, 2.0], [1500.0, 8.0], [1600.0, 5.0], [1700.0, 8.0], [1800.0, 11.0], [1900.0, 7.0], [2000.0, 12.0], [2100.0, 8.0], [2200.0, 4.0], [2300.0, 8.0], [2400.0, 5.0], [2500.0, 3.0], [2600.0, 6.0], [2700.0, 7.0], [2800.0, 7.0], [2900.0, 9.0], [3000.0, 8.0], [3100.0, 8.0], [3300.0, 25.0], [3200.0, 24.0], [3400.0, 34.0], [3500.0, 54.0], [3700.0, 143.0], [3600.0, 104.0], [3800.0, 133.0], [3900.0, 103.0], [4000.0, 96.0], [4100.0, 72.0], [4300.0, 65.0], [4200.0, 54.0], [4400.0, 61.0], [4500.0, 96.0], [4600.0, 69.0], [4700.0, 54.0], [4800.0, 46.0], [4900.0, 18.0], [5000.0, 17.0], [5100.0, 6.0], [5200.0, 3.0], [5400.0, 1.0], [6600.0, 1.0], [6800.0, 1.0], [7100.0, 1.0], [8000.0, 1.0], [500.0, 3.0]], "isOverall": false, "label": "GET /products by category", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 100, "maxX": 8000.0, "title": "Response Time Distribution"}},
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
        data: {"result": {"minY": 51.0, "minX": 1.0, "ticks": [[0, "Requests having \nresponse time <= 500ms"], [1, "Requests having \nresponse time > 500ms and <= 1,500ms"], [2, "Requests having \nresponse time > 1,500ms"], [3, "Requests in error"]], "maxY": 1406.0, "series": [{"data": [], "color": "#9ACD32", "isOverall": false, "label": "Requests having \nresponse time <= 500ms", "isController": false}, {"data": [[1.0, 51.0]], "color": "yellow", "isOverall": false, "label": "Requests having \nresponse time > 500ms and <= 1,500ms", "isController": false}, {"data": [[2.0, 1406.0]], "color": "orange", "isOverall": false, "label": "Requests having \nresponse time > 1,500ms", "isController": false}, {"data": [], "color": "#FF6347", "isOverall": false, "label": "Requests in error", "isController": false}], "supportsControllersDiscrimination": false, "maxX": 2.0, "title": "Synthetic Response Times Distribution"}},
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
        data: {"result": {"minY": 29.584699453551906, "minX": 1.78260012E12, "maxY": 49.99861111111106, "series": [{"data": [[1.78260024E12, 47.78880866425995], [1.78260012E12, 29.584699453551906], [1.78260018E12, 49.99861111111106]], "isOverall": false, "label": "Readers", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.78260024E12, "title": "Active Threads Over Time"}},
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
        data: {"result": {"minY": 1472.25, "minX": 1.0, "maxY": 4777.0, "series": [{"data": [[2.0, 4010.0], [3.0, 3764.0], [4.0, 4232.0], [5.0, 3693.0], [6.0, 4528.0], [7.0, 4513.0], [8.0, 3685.0], [9.0, 4777.0], [10.0, 3735.0], [11.0, 2489.7272727272725], [12.0, 2353.0], [13.0, 1472.25], [14.0, 1535.25], [15.0, 2897.5], [16.0, 1854.2], [17.0, 1544.7142857142858], [18.0, 1476.6], [19.0, 1625.5], [20.0, 2291.5], [21.0, 2051.125], [22.0, 1844.5555555555557], [23.0, 2005.25], [24.0, 1831.0], [25.0, 1812.6249999999998], [26.0, 2164.4], [27.0, 1890.0], [28.0, 2579.0], [29.0, 2735.3333333333335], [30.0, 2278.1111111111113], [31.0, 2385.4], [32.0, 2599.833333333333], [33.0, 2588.875], [34.0, 2371.1666666666665], [35.0, 2514.5], [36.0, 2593.714285714286], [37.0, 3095.25], [38.0, 2637.0], [39.0, 3030.75], [40.0, 3100.5], [41.0, 2875.1428571428573], [42.0, 3247.6666666666665], [43.0, 3322.25], [44.0, 3206.75], [45.0, 3967.0], [46.0, 3494.0], [47.0, 3287.142857142857], [48.0, 3412.5], [49.0, 3666.6666666666665], [50.0, 4105.838235294119], [1.0, 3997.0]], "isOverall": false, "label": "GET /products by category", "isController": false}, {"data": [[46.59437199725463, 3853.2367879203775]], "isOverall": false, "label": "GET /products by category-Aggregated", "isController": false}], "supportsControllersDiscrimination": true, "maxX": 50.0, "title": "Time VS Threads"}},
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
        data : {"result": {"minY": 545.95, "minX": 1.78260012E12, "maxY": 8.9211456E7, "series": [{"data": [[1.78260024E12, 6.86432592E7], [1.78260012E12, 2.26745784E7], [1.78260018E12, 8.9211456E7]], "isOverall": false, "label": "Bytes received per second", "isController": false}, {"data": [[1.78260024E12, 1652.7666666666667], [1.78260012E12, 545.95], [1.78260018E12, 2148.0]], "isOverall": false, "label": "Bytes sent per second", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.78260024E12, "title": "Bytes Throughput Over Time"}},
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
        data: {"result": {"minY": 2127.2841530054643, "minX": 1.78260012E12, "maxY": 4133.236111111109, "series": [{"data": [[1.78260024E12, 4059.4638989169657], [1.78260012E12, 2127.2841530054643], [1.78260018E12, 4133.236111111109]], "isOverall": false, "label": "GET /products by category", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.78260024E12, "title": "Response Time Over Time"}},
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
        data: {"result": {"minY": 2019.6721311475408, "minX": 1.78260012E12, "maxY": 4070.1625000000004, "series": [{"data": [[1.78260024E12, 3997.882671480147], [1.78260012E12, 2019.6721311475408], [1.78260018E12, 4070.1625000000004]], "isOverall": false, "label": "GET /products by category", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.78260024E12, "title": "Latencies Over Time"}},
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
        data: {"result": {"minY": 0.01624548736462094, "minX": 1.78260012E12, "maxY": 0.17486338797814202, "series": [{"data": [[1.78260024E12, 0.01624548736462094], [1.78260012E12, 0.17486338797814202], [1.78260018E12, 0.04027777777777779]], "isOverall": false, "label": "GET /products by category", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.78260024E12, "title": "Connect Time Over Time"}},
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
        data: {"result": {"minY": 507.0, "minX": 1.78260012E12, "maxY": 8002.0, "series": [{"data": [[1.78260024E12, 8002.0], [1.78260012E12, 4143.0], [1.78260018E12, 7143.0]], "isOverall": false, "label": "Max", "isController": false}, {"data": [[1.78260024E12, 508.0], [1.78260012E12, 571.0], [1.78260018E12, 507.0]], "isOverall": false, "label": "Min", "isController": false}, {"data": [[1.78260024E12, 4671.5], [1.78260012E12, 3323.0], [1.78260018E12, 4787.0]], "isOverall": false, "label": "90th percentile", "isController": false}, {"data": [[1.78260024E12, 5101.700000000004], [1.78260012E12, 3920.399999999999], [1.78260018E12, 5165.69]], "isOverall": false, "label": "99th percentile", "isController": false}, {"data": [[1.78260024E12, 3957.0], [1.78260012E12, 2058.0], [1.78260018E12, 4061.0]], "isOverall": false, "label": "Median", "isController": false}, {"data": [[1.78260024E12, 4811.75], [1.78260012E12, 3613.6], [1.78260018E12, 4905.799999999999]], "isOverall": false, "label": "95th percentile", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.78260024E12, "title": "Response Time Percentiles Over Time (successful requests only)"}},
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
    data: {"result": {"minY": 2305.5, "minX": 6.0, "maxY": 4178.0, "series": [{"data": [[8.0, 3446.5], [9.0, 3861.0], [10.0, 3856.0], [11.0, 3958.0], [12.0, 3922.0], [13.0, 3952.0], [14.0, 4046.5], [15.0, 3710.5], [16.0, 3743.5], [17.0, 4115.0], [18.0, 4178.0], [6.0, 3851.5], [7.0, 2305.5]], "isOverall": false, "label": "Successes", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 1000, "maxX": 18.0, "title": "Response Time Vs Request"}},
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
    data: {"result": {"minY": 2241.0, "minX": 6.0, "maxY": 4027.0, "series": [{"data": [[8.0, 3405.0], [9.0, 3823.0], [10.0, 3806.5], [11.0, 3907.5], [12.0, 3870.5], [13.0, 3881.0], [14.0, 3955.0], [15.0, 3672.0], [16.0, 3699.5], [17.0, 4027.0], [18.0, 4025.0], [6.0, 3792.0], [7.0, 2241.0]], "isOverall": false, "label": "Successes", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 1000, "maxX": 18.0, "title": "Latencies Vs Request"}},
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
        data: {"result": {"minY": 3.8666666666666667, "minX": 1.78260012E12, "maxY": 12.016666666666667, "series": [{"data": [[1.78260024E12, 8.4], [1.78260012E12, 3.8666666666666667], [1.78260018E12, 12.016666666666667]], "isOverall": false, "label": "hitsPerSecond", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.78260024E12, "title": "Hits Per Second"}},
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
        data: {"result": {"minY": 3.05, "minX": 1.78260012E12, "maxY": 12.0, "series": [{"data": [[1.78260024E12, 9.233333333333333], [1.78260012E12, 3.05], [1.78260018E12, 12.0]], "isOverall": false, "label": "200", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.78260024E12, "title": "Codes Per Second"}},
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
        data: {"result": {"minY": 3.05, "minX": 1.78260012E12, "maxY": 12.0, "series": [{"data": [[1.78260024E12, 9.233333333333333], [1.78260012E12, 3.05], [1.78260018E12, 12.0]], "isOverall": false, "label": "GET /products by category-success", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.78260024E12, "title": "Transactions Per Second"}},
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
        data: {"result": {"minY": 3.05, "minX": 1.78260012E12, "maxY": 12.0, "series": [{"data": [[1.78260024E12, 9.233333333333333], [1.78260012E12, 3.05], [1.78260018E12, 12.0]], "isOverall": false, "label": "Transaction-success", "isController": false}, {"data": [], "isOverall": false, "label": "Transaction-failure", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.78260024E12, "title": "Total Transactions Per Second"}},
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

