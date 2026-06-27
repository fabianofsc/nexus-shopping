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
        data: {"result": {"minY": 124.0, "minX": 0.0, "maxY": 4215.0, "series": [{"data": [[0.0, 124.0], [0.1, 148.0], [0.2, 154.0], [0.3, 157.0], [0.4, 167.0], [0.5, 184.0], [0.6, 187.0], [0.7, 193.0], [0.8, 206.0], [0.9, 210.0], [1.0, 215.0], [1.1, 223.0], [1.2, 232.0], [1.3, 234.0], [1.4, 243.0], [1.5, 246.0], [1.6, 250.0], [1.7, 265.0], [1.8, 271.0], [1.9, 287.0], [2.0, 288.0], [2.1, 293.0], [2.2, 297.0], [2.3, 310.0], [2.4, 314.0], [2.5, 322.0], [2.6, 334.0], [2.7, 343.0], [2.8, 346.0], [2.9, 352.0], [3.0, 363.0], [3.1, 384.0], [3.2, 392.0], [3.3, 393.0], [3.4, 397.0], [3.5, 399.0], [3.6, 402.0], [3.7, 409.0], [3.8, 412.0], [3.9, 417.0], [4.0, 426.0], [4.1, 430.0], [4.2, 439.0], [4.3, 447.0], [4.4, 447.0], [4.5, 453.0], [4.6, 456.0], [4.7, 470.0], [4.8, 473.0], [4.9, 481.0], [5.0, 488.0], [5.1, 489.0], [5.2, 489.0], [5.3, 496.0], [5.4, 499.0], [5.5, 516.0], [5.6, 522.0], [5.7, 541.0], [5.8, 546.0], [5.9, 552.0], [6.0, 557.0], [6.1, 561.0], [6.2, 564.0], [6.3, 565.0], [6.4, 566.0], [6.5, 566.0], [6.6, 569.0], [6.7, 574.0], [6.8, 582.0], [6.9, 585.0], [7.0, 587.0], [7.1, 591.0], [7.2, 595.0], [7.3, 599.0], [7.4, 600.0], [7.5, 603.0], [7.6, 611.0], [7.7, 616.0], [7.8, 617.0], [7.9, 620.0], [8.0, 623.0], [8.1, 627.0], [8.2, 631.0], [8.3, 631.0], [8.4, 635.0], [8.5, 642.0], [8.6, 647.0], [8.7, 651.0], [8.8, 655.0], [8.9, 662.0], [9.0, 664.0], [9.1, 667.0], [9.2, 668.0], [9.3, 669.0], [9.4, 672.0], [9.5, 673.0], [9.6, 682.0], [9.7, 686.0], [9.8, 688.0], [9.9, 689.0], [10.0, 692.0], [10.1, 694.0], [10.2, 697.0], [10.3, 700.0], [10.4, 700.0], [10.5, 705.0], [10.6, 711.0], [10.7, 715.0], [10.8, 719.0], [10.9, 723.0], [11.0, 724.0], [11.1, 726.0], [11.2, 728.0], [11.3, 731.0], [11.4, 736.0], [11.5, 740.0], [11.6, 742.0], [11.7, 746.0], [11.8, 754.0], [11.9, 761.0], [12.0, 766.0], [12.1, 769.0], [12.2, 771.0], [12.3, 777.0], [12.4, 788.0], [12.5, 790.0], [12.6, 794.0], [12.7, 796.0], [12.8, 798.0], [12.9, 799.0], [13.0, 803.0], [13.1, 804.0], [13.2, 811.0], [13.3, 814.0], [13.4, 816.0], [13.5, 816.0], [13.6, 822.0], [13.7, 824.0], [13.8, 827.0], [13.9, 829.0], [14.0, 831.0], [14.1, 836.0], [14.2, 846.0], [14.3, 853.0], [14.4, 856.0], [14.5, 864.0], [14.6, 866.0], [14.7, 869.0], [14.8, 887.0], [14.9, 895.0], [15.0, 900.0], [15.1, 900.0], [15.2, 903.0], [15.3, 904.0], [15.4, 908.0], [15.5, 911.0], [15.6, 912.0], [15.7, 915.0], [15.8, 915.0], [15.9, 917.0], [16.0, 920.0], [16.1, 922.0], [16.2, 923.0], [16.3, 926.0], [16.4, 930.0], [16.5, 933.0], [16.6, 937.0], [16.7, 944.0], [16.8, 948.0], [16.9, 950.0], [17.0, 954.0], [17.1, 957.0], [17.2, 964.0], [17.3, 970.0], [17.4, 973.0], [17.5, 975.0], [17.6, 977.0], [17.7, 979.0], [17.8, 979.0], [17.9, 981.0], [18.0, 983.0], [18.1, 986.0], [18.2, 987.0], [18.3, 988.0], [18.4, 990.0], [18.5, 992.0], [18.6, 994.0], [18.7, 997.0], [18.8, 997.0], [18.9, 999.0], [19.0, 1000.0], [19.1, 1002.0], [19.2, 1002.0], [19.3, 1004.0], [19.4, 1009.0], [19.5, 1010.0], [19.6, 1020.0], [19.7, 1022.0], [19.8, 1023.0], [19.9, 1029.0], [20.0, 1034.0], [20.1, 1036.0], [20.2, 1040.0], [20.3, 1042.0], [20.4, 1043.0], [20.5, 1044.0], [20.6, 1044.0], [20.7, 1046.0], [20.8, 1050.0], [20.9, 1051.0], [21.0, 1053.0], [21.1, 1059.0], [21.2, 1064.0], [21.3, 1068.0], [21.4, 1072.0], [21.5, 1076.0], [21.6, 1076.0], [21.7, 1078.0], [21.8, 1079.0], [21.9, 1080.0], [22.0, 1081.0], [22.1, 1084.0], [22.2, 1090.0], [22.3, 1093.0], [22.4, 1097.0], [22.5, 1102.0], [22.6, 1106.0], [22.7, 1110.0], [22.8, 1113.0], [22.9, 1117.0], [23.0, 1121.0], [23.1, 1123.0], [23.2, 1127.0], [23.3, 1132.0], [23.4, 1133.0], [23.5, 1135.0], [23.6, 1140.0], [23.7, 1145.0], [23.8, 1147.0], [23.9, 1149.0], [24.0, 1152.0], [24.1, 1155.0], [24.2, 1158.0], [24.3, 1162.0], [24.4, 1163.0], [24.5, 1166.0], [24.6, 1169.0], [24.7, 1171.0], [24.8, 1172.0], [24.9, 1174.0], [25.0, 1176.0], [25.1, 1180.0], [25.2, 1183.0], [25.3, 1186.0], [25.4, 1190.0], [25.5, 1194.0], [25.6, 1196.0], [25.7, 1199.0], [25.8, 1209.0], [25.9, 1214.0], [26.0, 1216.0], [26.1, 1221.0], [26.2, 1224.0], [26.3, 1225.0], [26.4, 1232.0], [26.5, 1236.0], [26.6, 1244.0], [26.7, 1248.0], [26.8, 1255.0], [26.9, 1263.0], [27.0, 1269.0], [27.1, 1269.0], [27.2, 1270.0], [27.3, 1271.0], [27.4, 1276.0], [27.5, 1279.0], [27.6, 1286.0], [27.7, 1289.0], [27.8, 1290.0], [27.9, 1291.0], [28.0, 1292.0], [28.1, 1295.0], [28.2, 1302.0], [28.3, 1304.0], [28.4, 1308.0], [28.5, 1309.0], [28.6, 1311.0], [28.7, 1313.0], [28.8, 1316.0], [28.9, 1318.0], [29.0, 1321.0], [29.1, 1321.0], [29.2, 1323.0], [29.3, 1326.0], [29.4, 1328.0], [29.5, 1330.0], [29.6, 1332.0], [29.7, 1333.0], [29.8, 1335.0], [29.9, 1341.0], [30.0, 1346.0], [30.1, 1353.0], [30.2, 1356.0], [30.3, 1366.0], [30.4, 1370.0], [30.5, 1378.0], [30.6, 1380.0], [30.7, 1381.0], [30.8, 1384.0], [30.9, 1386.0], [31.0, 1392.0], [31.1, 1395.0], [31.2, 1397.0], [31.3, 1402.0], [31.4, 1405.0], [31.5, 1406.0], [31.6, 1408.0], [31.7, 1409.0], [31.8, 1411.0], [31.9, 1412.0], [32.0, 1414.0], [32.1, 1414.0], [32.2, 1420.0], [32.3, 1420.0], [32.4, 1422.0], [32.5, 1423.0], [32.6, 1425.0], [32.7, 1429.0], [32.8, 1435.0], [32.9, 1437.0], [33.0, 1437.0], [33.1, 1439.0], [33.2, 1442.0], [33.3, 1443.0], [33.4, 1446.0], [33.5, 1450.0], [33.6, 1451.0], [33.7, 1453.0], [33.8, 1453.0], [33.9, 1455.0], [34.0, 1457.0], [34.1, 1462.0], [34.2, 1465.0], [34.3, 1466.0], [34.4, 1468.0], [34.5, 1470.0], [34.6, 1473.0], [34.7, 1475.0], [34.8, 1479.0], [34.9, 1482.0], [35.0, 1486.0], [35.1, 1488.0], [35.2, 1490.0], [35.3, 1494.0], [35.4, 1496.0], [35.5, 1497.0], [35.6, 1499.0], [35.7, 1502.0], [35.8, 1504.0], [35.9, 1505.0], [36.0, 1507.0], [36.1, 1511.0], [36.2, 1519.0], [36.3, 1530.0], [36.4, 1531.0], [36.5, 1534.0], [36.6, 1536.0], [36.7, 1538.0], [36.8, 1539.0], [36.9, 1539.0], [37.0, 1541.0], [37.1, 1544.0], [37.2, 1547.0], [37.3, 1549.0], [37.4, 1551.0], [37.5, 1556.0], [37.6, 1560.0], [37.7, 1567.0], [37.8, 1569.0], [37.9, 1570.0], [38.0, 1571.0], [38.1, 1572.0], [38.2, 1572.0], [38.3, 1573.0], [38.4, 1574.0], [38.5, 1576.0], [38.6, 1577.0], [38.7, 1579.0], [38.8, 1581.0], [38.9, 1583.0], [39.0, 1585.0], [39.1, 1586.0], [39.2, 1588.0], [39.3, 1589.0], [39.4, 1594.0], [39.5, 1595.0], [39.6, 1598.0], [39.7, 1599.0], [39.8, 1600.0], [39.9, 1601.0], [40.0, 1603.0], [40.1, 1604.0], [40.2, 1606.0], [40.3, 1608.0], [40.4, 1610.0], [40.5, 1611.0], [40.6, 1613.0], [40.7, 1613.0], [40.8, 1614.0], [40.9, 1616.0], [41.0, 1616.0], [41.1, 1617.0], [41.2, 1618.0], [41.3, 1620.0], [41.4, 1622.0], [41.5, 1626.0], [41.6, 1626.0], [41.7, 1630.0], [41.8, 1633.0], [41.9, 1635.0], [42.0, 1637.0], [42.1, 1639.0], [42.2, 1643.0], [42.3, 1647.0], [42.4, 1648.0], [42.5, 1651.0], [42.6, 1655.0], [42.7, 1659.0], [42.8, 1660.0], [42.9, 1661.0], [43.0, 1663.0], [43.1, 1664.0], [43.2, 1667.0], [43.3, 1668.0], [43.4, 1668.0], [43.5, 1671.0], [43.6, 1672.0], [43.7, 1674.0], [43.8, 1676.0], [43.9, 1678.0], [44.0, 1679.0], [44.1, 1680.0], [44.2, 1682.0], [44.3, 1683.0], [44.4, 1686.0], [44.5, 1686.0], [44.6, 1690.0], [44.7, 1694.0], [44.8, 1696.0], [44.9, 1697.0], [45.0, 1698.0], [45.1, 1699.0], [45.2, 1699.0], [45.3, 1700.0], [45.4, 1701.0], [45.5, 1702.0], [45.6, 1702.0], [45.7, 1703.0], [45.8, 1705.0], [45.9, 1705.0], [46.0, 1706.0], [46.1, 1706.0], [46.2, 1706.0], [46.3, 1707.0], [46.4, 1707.0], [46.5, 1708.0], [46.6, 1709.0], [46.7, 1709.0], [46.8, 1710.0], [46.9, 1710.0], [47.0, 1711.0], [47.1, 1713.0], [47.2, 1714.0], [47.3, 1714.0], [47.4, 1716.0], [47.5, 1718.0], [47.6, 1718.0], [47.7, 1719.0], [47.8, 1721.0], [47.9, 1721.0], [48.0, 1722.0], [48.1, 1724.0], [48.2, 1726.0], [48.3, 1727.0], [48.4, 1728.0], [48.5, 1728.0], [48.6, 1730.0], [48.7, 1731.0], [48.8, 1735.0], [48.9, 1737.0], [49.0, 1740.0], [49.1, 1741.0], [49.2, 1741.0], [49.3, 1743.0], [49.4, 1747.0], [49.5, 1750.0], [49.6, 1751.0], [49.7, 1754.0], [49.8, 1755.0], [49.9, 1756.0], [50.0, 1758.0], [50.1, 1758.0], [50.2, 1760.0], [50.3, 1766.0], [50.4, 1769.0], [50.5, 1772.0], [50.6, 1774.0], [50.7, 1777.0], [50.8, 1780.0], [50.9, 1783.0], [51.0, 1786.0], [51.1, 1787.0], [51.2, 1788.0], [51.3, 1789.0], [51.4, 1791.0], [51.5, 1793.0], [51.6, 1795.0], [51.7, 1797.0], [51.8, 1799.0], [51.9, 1799.0], [52.0, 1800.0], [52.1, 1802.0], [52.2, 1803.0], [52.3, 1804.0], [52.4, 1807.0], [52.5, 1810.0], [52.6, 1812.0], [52.7, 1813.0], [52.8, 1814.0], [52.9, 1819.0], [53.0, 1820.0], [53.1, 1822.0], [53.2, 1824.0], [53.3, 1827.0], [53.4, 1829.0], [53.5, 1830.0], [53.6, 1832.0], [53.7, 1833.0], [53.8, 1833.0], [53.9, 1835.0], [54.0, 1837.0], [54.1, 1839.0], [54.2, 1841.0], [54.3, 1844.0], [54.4, 1846.0], [54.5, 1847.0], [54.6, 1847.0], [54.7, 1848.0], [54.8, 1849.0], [54.9, 1849.0], [55.0, 1849.0], [55.1, 1850.0], [55.2, 1850.0], [55.3, 1851.0], [55.4, 1851.0], [55.5, 1851.0], [55.6, 1852.0], [55.7, 1852.0], [55.8, 1853.0], [55.9, 1853.0], [56.0, 1855.0], [56.1, 1861.0], [56.2, 1863.0], [56.3, 1865.0], [56.4, 1868.0], [56.5, 1869.0], [56.6, 1872.0], [56.7, 1874.0], [56.8, 1876.0], [56.9, 1878.0], [57.0, 1879.0], [57.1, 1881.0], [57.2, 1883.0], [57.3, 1885.0], [57.4, 1886.0], [57.5, 1889.0], [57.6, 1890.0], [57.7, 1892.0], [57.8, 1894.0], [57.9, 1895.0], [58.0, 1895.0], [58.1, 1896.0], [58.2, 1896.0], [58.3, 1897.0], [58.4, 1898.0], [58.5, 1899.0], [58.6, 1900.0], [58.7, 1901.0], [58.8, 1902.0], [58.9, 1902.0], [59.0, 1903.0], [59.1, 1904.0], [59.2, 1905.0], [59.3, 1905.0], [59.4, 1907.0], [59.5, 1907.0], [59.6, 1909.0], [59.7, 1911.0], [59.8, 1912.0], [59.9, 1913.0], [60.0, 1915.0], [60.1, 1916.0], [60.2, 1916.0], [60.3, 1917.0], [60.4, 1917.0], [60.5, 1917.0], [60.6, 1918.0], [60.7, 1919.0], [60.8, 1920.0], [60.9, 1920.0], [61.0, 1921.0], [61.1, 1922.0], [61.2, 1922.0], [61.3, 1923.0], [61.4, 1924.0], [61.5, 1924.0], [61.6, 1925.0], [61.7, 1926.0], [61.8, 1929.0], [61.9, 1930.0], [62.0, 1930.0], [62.1, 1931.0], [62.2, 1932.0], [62.3, 1932.0], [62.4, 1933.0], [62.5, 1934.0], [62.6, 1938.0], [62.7, 1939.0], [62.8, 1941.0], [62.9, 1945.0], [63.0, 1946.0], [63.1, 1947.0], [63.2, 1948.0], [63.3, 1950.0], [63.4, 1953.0], [63.5, 1954.0], [63.6, 1955.0], [63.7, 1956.0], [63.8, 1958.0], [63.9, 1959.0], [64.0, 1959.0], [64.1, 1960.0], [64.2, 1962.0], [64.3, 1964.0], [64.4, 1965.0], [64.5, 1967.0], [64.6, 1968.0], [64.7, 1969.0], [64.8, 1970.0], [64.9, 1971.0], [65.0, 1971.0], [65.1, 1973.0], [65.2, 1975.0], [65.3, 1976.0], [65.4, 1978.0], [65.5, 1980.0], [65.6, 1984.0], [65.7, 1986.0], [65.8, 1988.0], [65.9, 1989.0], [66.0, 1992.0], [66.1, 1993.0], [66.2, 1993.0], [66.3, 1995.0], [66.4, 1996.0], [66.5, 1997.0], [66.6, 2000.0], [66.7, 2001.0], [66.8, 2003.0], [66.9, 2004.0], [67.0, 2006.0], [67.1, 2007.0], [67.2, 2009.0], [67.3, 2010.0], [67.4, 2011.0], [67.5, 2014.0], [67.6, 2017.0], [67.7, 2018.0], [67.8, 2020.0], [67.9, 2022.0], [68.0, 2023.0], [68.1, 2024.0], [68.2, 2025.0], [68.3, 2025.0], [68.4, 2028.0], [68.5, 2030.0], [68.6, 2033.0], [68.7, 2037.0], [68.8, 2047.0], [68.9, 2049.0], [69.0, 2049.0], [69.1, 2051.0], [69.2, 2056.0], [69.3, 2059.0], [69.4, 2062.0], [69.5, 2064.0], [69.6, 2066.0], [69.7, 2069.0], [69.8, 2074.0], [69.9, 2075.0], [70.0, 2076.0], [70.1, 2077.0], [70.2, 2082.0], [70.3, 2084.0], [70.4, 2086.0], [70.5, 2089.0], [70.6, 2094.0], [70.7, 2095.0], [70.8, 2096.0], [70.9, 2097.0], [71.0, 2097.0], [71.1, 2098.0], [71.2, 2103.0], [71.3, 2104.0], [71.4, 2106.0], [71.5, 2109.0], [71.6, 2110.0], [71.7, 2111.0], [71.8, 2112.0], [71.9, 2114.0], [72.0, 2120.0], [72.1, 2121.0], [72.2, 2124.0], [72.3, 2127.0], [72.4, 2134.0], [72.5, 2137.0], [72.6, 2139.0], [72.7, 2140.0], [72.8, 2141.0], [72.9, 2144.0], [73.0, 2145.0], [73.1, 2146.0], [73.2, 2147.0], [73.3, 2148.0], [73.4, 2150.0], [73.5, 2151.0], [73.6, 2154.0], [73.7, 2159.0], [73.8, 2164.0], [73.9, 2167.0], [74.0, 2168.0], [74.1, 2170.0], [74.2, 2173.0], [74.3, 2176.0], [74.4, 2180.0], [74.5, 2182.0], [74.6, 2183.0], [74.7, 2187.0], [74.8, 2190.0], [74.9, 2194.0], [75.0, 2200.0], [75.1, 2202.0], [75.2, 2203.0], [75.3, 2206.0], [75.4, 2207.0], [75.5, 2208.0], [75.6, 2209.0], [75.7, 2215.0], [75.8, 2217.0], [75.9, 2221.0], [76.0, 2224.0], [76.1, 2226.0], [76.2, 2226.0], [76.3, 2228.0], [76.4, 2230.0], [76.5, 2233.0], [76.6, 2235.0], [76.7, 2238.0], [76.8, 2240.0], [76.9, 2243.0], [77.0, 2250.0], [77.1, 2256.0], [77.2, 2259.0], [77.3, 2262.0], [77.4, 2264.0], [77.5, 2266.0], [77.6, 2266.0], [77.7, 2267.0], [77.8, 2267.0], [77.9, 2268.0], [78.0, 2268.0], [78.1, 2268.0], [78.2, 2269.0], [78.3, 2270.0], [78.4, 2271.0], [78.5, 2272.0], [78.6, 2273.0], [78.7, 2273.0], [78.8, 2274.0], [78.9, 2277.0], [79.0, 2282.0], [79.1, 2284.0], [79.2, 2288.0], [79.3, 2293.0], [79.4, 2296.0], [79.5, 2299.0], [79.6, 2302.0], [79.7, 2310.0], [79.8, 2313.0], [79.9, 2316.0], [80.0, 2322.0], [80.1, 2325.0], [80.2, 2327.0], [80.3, 2331.0], [80.4, 2334.0], [80.5, 2335.0], [80.6, 2336.0], [80.7, 2339.0], [80.8, 2340.0], [80.9, 2343.0], [81.0, 2352.0], [81.1, 2352.0], [81.2, 2356.0], [81.3, 2361.0], [81.4, 2363.0], [81.5, 2364.0], [81.6, 2367.0], [81.7, 2369.0], [81.8, 2369.0], [81.9, 2370.0], [82.0, 2371.0], [82.1, 2371.0], [82.2, 2372.0], [82.3, 2374.0], [82.4, 2381.0], [82.5, 2385.0], [82.6, 2387.0], [82.7, 2387.0], [82.8, 2388.0], [82.9, 2394.0], [83.0, 2395.0], [83.1, 2396.0], [83.2, 2398.0], [83.3, 2402.0], [83.4, 2404.0], [83.5, 2409.0], [83.6, 2414.0], [83.7, 2415.0], [83.8, 2420.0], [83.9, 2422.0], [84.0, 2425.0], [84.1, 2433.0], [84.2, 2450.0], [84.3, 2460.0], [84.4, 2482.0], [84.5, 2491.0], [84.6, 2500.0], [84.7, 2502.0], [84.8, 2503.0], [84.9, 2503.0], [85.0, 2505.0], [85.1, 2506.0], [85.2, 2506.0], [85.3, 2507.0], [85.4, 2508.0], [85.5, 2509.0], [85.6, 2510.0], [85.7, 2512.0], [85.8, 2513.0], [85.9, 2514.0], [86.0, 2517.0], [86.1, 2518.0], [86.2, 2518.0], [86.3, 2519.0], [86.4, 2520.0], [86.5, 2521.0], [86.6, 2521.0], [86.7, 2523.0], [86.8, 2525.0], [86.9, 2528.0], [87.0, 2531.0], [87.1, 2534.0], [87.2, 2541.0], [87.3, 2547.0], [87.4, 2561.0], [87.5, 2567.0], [87.6, 2574.0], [87.7, 2577.0], [87.8, 2591.0], [87.9, 2595.0], [88.0, 2596.0], [88.1, 2608.0], [88.2, 2613.0], [88.3, 2624.0], [88.4, 2628.0], [88.5, 2629.0], [88.6, 2635.0], [88.7, 2645.0], [88.8, 2653.0], [88.9, 2661.0], [89.0, 2665.0], [89.1, 2666.0], [89.2, 2669.0], [89.3, 2674.0], [89.4, 2676.0], [89.5, 2693.0], [89.6, 2711.0], [89.7, 2716.0], [89.8, 2721.0], [89.9, 2734.0], [90.0, 2737.0], [90.1, 2743.0], [90.2, 2748.0], [90.3, 2750.0], [90.4, 2755.0], [90.5, 2758.0], [90.6, 2764.0], [90.7, 2765.0], [90.8, 2767.0], [90.9, 2782.0], [91.0, 2796.0], [91.1, 2796.0], [91.2, 2799.0], [91.3, 2801.0], [91.4, 2801.0], [91.5, 2802.0], [91.6, 2804.0], [91.7, 2805.0], [91.8, 2806.0], [91.9, 2808.0], [92.0, 2809.0], [92.1, 2810.0], [92.2, 2811.0], [92.3, 2815.0], [92.4, 2819.0], [92.5, 2821.0], [92.6, 2822.0], [92.7, 2826.0], [92.8, 2832.0], [92.9, 2840.0], [93.0, 2843.0], [93.1, 2849.0], [93.2, 2852.0], [93.3, 2855.0], [93.4, 2857.0], [93.5, 2859.0], [93.6, 2862.0], [93.7, 2865.0], [93.8, 2866.0], [93.9, 2870.0], [94.0, 2873.0], [94.1, 2875.0], [94.2, 2898.0], [94.3, 2906.0], [94.4, 2912.0], [94.5, 2915.0], [94.6, 2919.0], [94.7, 2921.0], [94.8, 2922.0], [94.9, 2924.0], [95.0, 2930.0], [95.1, 2936.0], [95.2, 2938.0], [95.3, 2939.0], [95.4, 2941.0], [95.5, 2975.0], [95.6, 2985.0], [95.7, 2997.0], [95.8, 3007.0], [95.9, 3010.0], [96.0, 3012.0], [96.1, 3014.0], [96.2, 3017.0], [96.3, 3071.0], [96.4, 3082.0], [96.5, 3128.0], [96.6, 3141.0], [96.7, 3144.0], [96.8, 3148.0], [96.9, 3177.0], [97.0, 3185.0], [97.1, 3245.0], [97.2, 3262.0], [97.3, 3299.0], [97.4, 3308.0], [97.5, 3312.0], [97.6, 3379.0], [97.7, 3387.0], [97.8, 3400.0], [97.9, 3419.0], [98.0, 3454.0], [98.1, 3542.0], [98.2, 3544.0], [98.3, 3544.0], [98.4, 3568.0], [98.5, 3571.0], [98.6, 3576.0], [98.7, 3589.0], [98.8, 3672.0], [98.9, 3711.0], [99.0, 3751.0], [99.1, 3758.0], [99.2, 3774.0], [99.3, 3791.0], [99.4, 3816.0], [99.5, 3827.0], [99.6, 3952.0], [99.7, 4072.0], [99.8, 4079.0], [99.9, 4094.0]], "isOverall": false, "label": "GET /products by category", "isController": false}], "supportsControllersDiscrimination": true, "maxX": 100.0, "title": "Response Time Percentiles"}},
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
        data: {"result": {"minY": 1.0, "minX": 100.0, "maxY": 258.0, "series": [{"data": [[600.0, 95.0], [700.0, 85.0], [800.0, 67.0], [900.0, 128.0], [1000.0, 112.0], [1100.0, 106.0], [1200.0, 79.0], [1300.0, 99.0], [1400.0, 140.0], [1500.0, 133.0], [100.0, 23.0], [1600.0, 177.0], [1700.0, 216.0], [1800.0, 212.0], [1900.0, 258.0], [2000.0, 149.0], [2100.0, 121.0], [2200.0, 149.0], [2300.0, 119.0], [2400.0, 42.0], [2500.0, 113.0], [2600.0, 48.0], [2700.0, 55.0], [2800.0, 96.0], [2900.0, 48.0], [3000.0, 25.0], [3100.0, 17.0], [200.0, 50.0], [3300.0, 14.0], [3200.0, 11.0], [3400.0, 8.0], [3500.0, 23.0], [3700.0, 16.0], [3600.0, 2.0], [3800.0, 8.0], [3900.0, 3.0], [4000.0, 8.0], [4200.0, 2.0], [4100.0, 1.0], [300.0, 40.0], [400.0, 61.0], [500.0, 62.0]], "isOverall": false, "label": "GET /products by category", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 100, "maxX": 4200.0, "title": "Response Time Distribution"}},
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
        data: {"result": {"minY": 174.0, "minX": 0.0, "ticks": [[0, "Requests having \nresponse time <= 500ms"], [1, "Requests having \nresponse time > 500ms and <= 1,500ms"], [2, "Requests having \nresponse time > 1,500ms"], [3, "Requests in error"]], "maxY": 2074.0, "series": [{"data": [[0.0, 174.0]], "color": "#9ACD32", "isOverall": false, "label": "Requests having \nresponse time <= 500ms", "isController": false}, {"data": [[1.0, 973.0]], "color": "yellow", "isOverall": false, "label": "Requests having \nresponse time > 500ms and <= 1,500ms", "isController": false}, {"data": [[2.0, 2074.0]], "color": "orange", "isOverall": false, "label": "Requests having \nresponse time > 1,500ms", "isController": false}, {"data": [], "color": "#FF6347", "isOverall": false, "label": "Requests in error", "isController": false}], "supportsControllersDiscrimination": false, "maxX": 2.0, "title": "Synthetic Response Times Distribution"}},
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
        data: {"result": {"minY": 17.95512820512821, "minX": 1.78260282E12, "maxY": 49.269255251432284, "series": [{"data": [[1.78260282E12, 17.95512820512821], [1.78260294E12, 49.201046337817615], [1.78260288E12, 49.269255251432284]], "isOverall": false, "label": "Readers", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.78260294E12, "title": "Active Threads Over Time"}},
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
        data: {"result": {"minY": 184.4, "minX": 1.0, "maxY": 2780.0000000000005, "series": [{"data": [[2.0, 892.0], [33.0, 2300.25], [35.0, 1866.4374999999998], [37.0, 937.0], [36.0, 2162.0], [38.0, 1182.875], [39.0, 1676.4166666666667], [40.0, 1926.0500000000002], [43.0, 1303.3], [42.0, 1640.6], [44.0, 2077.5], [45.0, 2212.6923076923076], [46.0, 2780.0000000000005], [47.0, 1688.0], [3.0, 813.6666666666667], [48.0, 1563.1176470588234], [49.0, 1569.3636363636365], [50.0, 1852.3103195005506], [4.0, 423.0], [5.0, 255.55555555555554], [6.0, 399.0], [7.0, 184.4], [8.0, 421.2857142857143], [9.0, 227.11111111111114], [11.0, 748.1666666666667], [12.0, 309.84999999999997], [13.0, 1833.0], [14.0, 592.7499999999999], [15.0, 443.5789473684211], [16.0, 1896.0], [1.0, 1734.0], [17.0, 877.8235294117648], [18.0, 776.8999999999999], [19.0, 410.4761904761905], [20.0, 760.8888888888889], [21.0, 731.0], [22.0, 911.4545454545455], [23.0, 438.304347826087], [24.0, 1863.0], [25.0, 937.1304347826087], [26.0, 1344.5], [27.0, 1152.8333333333333], [28.0, 1103.6111111111115], [30.0, 1058.0833333333333], [31.0, 1252.388888888889]], "isOverall": false, "label": "GET /products by category", "isController": false}, {"data": [[46.207699472213534, 1731.1347407637418]], "isOverall": false, "label": "GET /products by category-Aggregated", "isController": false}], "supportsControllersDiscrimination": true, "maxX": 50.0, "title": "Time VS Threads"}},
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
        data : {"result": {"minY": 930.8, "minX": 1.78260282E12, "maxY": 1.946544408E8, "series": [{"data": [[1.78260282E12, 3.86582976E7], [1.78260294E12, 1.657846224E8], [1.78260288E12, 1.946544408E8]], "isOverall": false, "label": "Bytes received per second", "isController": false}, {"data": [[1.78260282E12, 930.8], [1.78260294E12, 3991.7], [1.78260288E12, 4686.816666666667]], "isOverall": false, "label": "Bytes sent per second", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.78260294E12, "title": "Bytes Throughput Over Time"}},
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
        data: {"result": {"minY": 592.5576923076926, "minX": 1.78260282E12, "maxY": 1880.8930617441108, "series": [{"data": [[1.78260282E12, 592.5576923076926], [1.78260294E12, 1820.7952167414041], [1.78260288E12, 1880.8930617441108]], "isOverall": false, "label": "GET /products by category", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.78260294E12, "title": "Response Time Over Time"}},
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
        data: {"result": {"minY": 459.9166666666666, "minX": 1.78260282E12, "maxY": 1545.7924888605978, "series": [{"data": [[1.78260282E12, 459.9166666666666], [1.78260294E12, 1493.4222720478324], [1.78260288E12, 1545.7924888605978]], "isOverall": false, "label": "GET /products by category", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.78260294E12, "title": "Latencies Over Time"}},
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
        data: {"result": {"minY": 0.01943198804185356, "minX": 1.78260282E12, "maxY": 0.05128205128205129, "series": [{"data": [[1.78260282E12, 0.05128205128205129], [1.78260294E12, 0.01943198804185356], [1.78260288E12, 0.036919159770846637]], "isOverall": false, "label": "GET /products by category", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.78260294E12, "title": "Connect Time Over Time"}},
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
        data: {"result": {"minY": 124.0, "minX": 1.78260282E12, "maxY": 4215.0, "series": [{"data": [[1.78260282E12, 1714.0], [1.78260294E12, 4101.0], [1.78260288E12, 4215.0]], "isOverall": false, "label": "Max", "isController": false}, {"data": [[1.78260282E12, 124.0], [1.78260294E12, 214.0], [1.78260288E12, 184.0]], "isOverall": false, "label": "Min", "isController": false}, {"data": [[1.78260282E12, 1081.7], [1.78260294E12, 2666.0], [1.78260288E12, 2857.0]], "isOverall": false, "label": "90th percentile", "isController": false}, {"data": [[1.78260282E12, 1441.96], [1.78260294E12, 3653.299999999987], [1.78260288E12, 3775.3999999999996]], "isOverall": false, "label": "99th percentile", "isController": false}, {"data": [[1.78260282E12, 495.5], [1.78260294E12, 1830.0], [1.78260288E12, 1876.0]], "isOverall": false, "label": "Median", "isController": false}, {"data": [[1.78260282E12, 1124.0], [1.78260294E12, 2813.0], [1.78260288E12, 3010.0]], "isOverall": false, "label": "95th percentile", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.78260294E12, "title": "Response Time Percentiles Over Time (successful requests only)"}},
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
    data: {"result": {"minY": 231.0, "minX": 2.0, "maxY": 2815.0, "series": [{"data": [[2.0, 477.5], [32.0, 1142.5], [33.0, 1290.0], [34.0, 447.0], [35.0, 1682.0], [36.0, 1503.5], [37.0, 1236.0], [38.0, 432.5], [39.0, 1877.0], [40.0, 2058.0], [41.0, 1922.0], [42.0, 1872.5], [44.0, 1499.0], [45.0, 1902.5], [47.0, 2132.0], [46.0, 1594.5], [3.0, 1721.0], [50.0, 2287.5], [55.0, 1741.0], [56.0, 1643.0], [59.0, 2815.0], [7.0, 1790.5], [9.0, 1404.0], [10.0, 1706.0], [12.0, 590.0], [13.0, 1572.5], [14.0, 2760.5], [15.0, 951.0], [18.0, 1673.5], [19.0, 1542.5], [20.0, 1300.5], [21.0, 1899.0], [22.0, 1110.0], [23.0, 1115.0], [24.0, 986.0], [25.0, 695.5], [26.0, 231.0], [27.0, 1286.0], [28.0, 1270.0], [29.0, 1010.0], [30.0, 1814.5], [31.0, 990.0]], "isOverall": false, "label": "Successes", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 1000, "maxX": 59.0, "title": "Response Time Vs Request"}},
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
    data: {"result": {"minY": 165.0, "minX": 2.0, "maxY": 2639.5, "series": [{"data": [[2.0, 272.0], [32.0, 932.5], [33.0, 1226.0], [34.0, 337.0], [35.0, 1590.0], [36.0, 1337.0], [37.0, 1185.0], [38.0, 359.5], [39.0, 1213.0], [40.0, 1557.0], [41.0, 1509.0], [42.0, 1506.5], [44.0, 1122.0], [45.0, 1268.0], [47.0, 1761.0], [46.0, 1295.5], [3.0, 1355.0], [50.0, 1631.5], [55.0, 1671.0], [56.0, 1325.5], [59.0, 2581.0], [7.0, 1501.0], [9.0, 676.0], [10.0, 1120.0], [12.0, 445.0], [13.0, 1473.5], [14.0, 2639.5], [15.0, 872.0], [18.0, 1475.5], [19.0, 1262.0], [20.0, 1224.0], [21.0, 1862.0], [22.0, 961.5], [23.0, 1048.0], [24.0, 783.0], [25.0, 653.0], [26.0, 165.0], [27.0, 1206.0], [28.0, 1060.0], [29.0, 966.0], [30.0, 1683.5], [31.0, 949.0]], "isOverall": false, "label": "Successes", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 1000, "maxX": 59.0, "title": "Latencies Vs Request"}},
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
        data: {"result": {"minY": 5.783333333333333, "minX": 1.78260282E12, "maxY": 26.433333333333334, "series": [{"data": [[1.78260282E12, 5.783333333333333], [1.78260294E12, 21.466666666666665], [1.78260288E12, 26.433333333333334]], "isOverall": false, "label": "hitsPerSecond", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.78260294E12, "title": "Hits Per Second"}},
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
        data: {"result": {"minY": 5.2, "minX": 1.78260282E12, "maxY": 26.183333333333334, "series": [{"data": [[1.78260282E12, 5.2], [1.78260294E12, 22.3], [1.78260288E12, 26.183333333333334]], "isOverall": false, "label": "200", "isController": false}], "supportsControllersDiscrimination": false, "granularity": 60000, "maxX": 1.78260294E12, "title": "Codes Per Second"}},
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
        data: {"result": {"minY": 5.2, "minX": 1.78260282E12, "maxY": 26.183333333333334, "series": [{"data": [[1.78260282E12, 5.2], [1.78260294E12, 22.3], [1.78260288E12, 26.183333333333334]], "isOverall": false, "label": "GET /products by category-success", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.78260294E12, "title": "Transactions Per Second"}},
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
        data: {"result": {"minY": 5.2, "minX": 1.78260282E12, "maxY": 26.183333333333334, "series": [{"data": [[1.78260282E12, 5.2], [1.78260294E12, 22.3], [1.78260288E12, 26.183333333333334]], "isOverall": false, "label": "Transaction-success", "isController": false}, {"data": [], "isOverall": false, "label": "Transaction-failure", "isController": false}], "supportsControllersDiscrimination": true, "granularity": 60000, "maxX": 1.78260294E12, "title": "Total Transactions Per Second"}},
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

