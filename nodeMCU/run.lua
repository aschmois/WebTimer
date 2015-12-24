local wifiConfig = {}
wifiConfig.staticIp = {}

if(file.open("config", "r") ~= nil) then
	local function read ()
		local buf = file.readline()
		if(buf ~= nil) then
			buf = string.gsub(buf, "\n", "")
		else
			buf = ""
		end
		return buf
	end
	
	wifiConfig.ssid = read()..""
	wifiConfig.ssidPassword = read()..""
	wifiConfig.staticIp.ip = read()..""
	wifiConfig.staticIp.netmask = read()..""
	wifiConfig.staticIp.gateway = read()..""
	
	print("-- CONFIGURATION --")
	print("SSID: " .. wifiConfig.ssid)
	print("Password: *****") -- .. wifiConfig.ssidPassword)
	print("IP: " .. wifiConfig.staticIp.ip)
	print("Netmask: " .. wifiConfig.staticIp.netmask)
	print("Gateway: " .. wifiConfig.staticIp.gateway)
	print("-- END CONFIGURATION --")
	file.close()
end

-- Lamp setup
local lamp = {}
lamp.pin = 4
lamp.status = 1
gpio.mode(lamp.pin, gpio.OUTPUT)
gpio.write(lamp.pin, gpio.LOW)

local function connect(conn)
   conn:on ("receive",
   	function (conn, request)
		print(request)
		local buf = ""
		local _, _, method, path, vars = string.find(request, "([A-Z]+) (.+)?(.+) HTTP")
        if(method == nil)then
            _, _, method, path = string.find(request, "([A-Z]+) (.+) HTTP")
        end
        local _GET = {}
        if (vars ~= nil)then
            for k, v in string.gmatch(vars, "(%w+)=(%w+)&*") do
                _GET[k] = v
            end
        end
        buf = buf.."HTTP/1.1 200/OK\r\nServer: WiFi Relay\r\nContent-Type: text/html\r\n\r\n<html>"
		if(_GET.light == "0" or _GET.pin == "OFF") then
			gpio.write(lamp.pin, gpio.HIGH)
			lamp.status = 0
	    elseif(_GET.light == "1" or _GET.pin == "ON") then
			gpio.write(lamp.pin, gpio.LOW)
			lamp.status = 1
        end
		if(_GET.setup == 1) then
			file.remove("config")
			node.restart()
		end
		if(_GET.status == "1") then
			buf = buf..lamp.status
		else
			buf = buf.."<h1> ESP8266 Web Server</h1>"
			buf = buf.."<p>LAMP <a href=\"?pin=ON\"><button>ON</button></a>&nbsp;<a href=\"?pin=OFF\"><button>OFF</button></a></p>"
			buf = buf.."<p>LAMP is <font color="
			if(lamp.status == 1) then
				buf = buf.."\"green\">ON"
			else
				buf = buf.."\"red\">OFF"
			end
			buf = buf.."</font></p>"
		end
		buf = buf.."</html>"
		conn:send(buf);
        conn:close()
    end)
end

wifi.setmode(wifi.STATION)
wifi.sta.config(wifiConfig.ssid, wifiConfig.ssidPassword)
wifi.sta.setip(wifiConfig.staticIp)

function check_wifi()
	wifi.sta.connect()
	local ip = wifi.sta.getip()

	if(ip==nil) then
		print("Connecting...")
	else
		tmr.stop(1)
		print("IP: " .. wifi.sta.getip())
		local svr = net.createServer(net.TCP)
		svr:listen(80, connect)
	end
end

tmr.alarm(1,3000,1,check_wifi)