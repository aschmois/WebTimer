-- Wifi connection data
local wifiConfig = {}
wifiConfig.staticIp = {}

if(file.open("config", "r") ~= nil) then
	local function read ()
		local buf = file.readline()
		if(buf ~= nil) then
			buf = string.gsub(buf, "n", "")
		else
			buf = ""
		end
		return buf
	end
	
	wifiConfig.ssid = read()
	wifiConfig.ssidPassword = read()
	wifiConfig.staticIp.ip = read()
	wifiConfig.staticIp.netmask = read()
	wifiConfig.staticIp.gateway = read()
	
	file.close()
	
end

-- Lamp setup
local lamp = {}
lamp.pin = 4 -- this is GPIO0
lamp.status = 1
gpio.mode(lamp.pin,gpio.OUTPUT)
gpio.write(lamp.pin,gpio.HIGH)

local function connect (conn, data)
   local query_data

   conn:on ("receive",
   	function (cn, request)
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
        cn:send("HTTP/1.1 200/OK\r\nServer: NodeLuau\r\nContent-Type: text/html\r\n\r\n")
		if(_GET.light == "0" or _GET.pin == "OFF") then
			gpio.write(lamp.pin, gpio.LOW)
			lamp.status = 0
	    elseif(_GET.light == "1" or _GET.pin == "ON") then
			gpio.write(lamp.pin, gpio.HIGH)
			lamp.status = 1
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
		cn:send(buf);
	  -- Close the connection for the request
        cn:close()
		collectgarbage()
    end)
end

-- Configure the ESP as a station (client)
wifi.setmode(wifi.STATION)
wifi.sta.config(wifiConfig.ssid, wifiConfig.ssidPassword)
wifi.sta.setip(wifiConfig.staticIp)

-- Hang out until we get a wifi connection before the httpd server is started.
tmr.alarm (1, 800, 1, function ( )
  if wifi.sta.getip ( ) == nil then
     print ("Waiting for Wifi connection")
  else
     tmr.stop (1)
     print ("Config done, IP is " .. wifi.sta.getip ( ))
  end
end)

local joinCounter = 0
local joinMaxAttempts = 5
tmr.alarm(0, 3000, 1, function()
    local ip = wifi.sta.getip()
    if ip == nil and joinCounter < joinMaxAttempts then
        print('Connecting to WiFi Access Point ...')
		joinCounter = joinCounter + 1
    else
		if joinCounter == joinMaxAttempts then
			print('Failed to connect to WiFi Access Point.')
		else
			print('IP: ',ip)
		end
		tmr.stop(0)
		joinCounter = nil
		joinMaxAttempts = nil
		collectgarbage()
	end
end)

-- Create the httpd server
svr = net.createServer (net.TCP, 30)

-- Server listening on port 80, call connect function if a request is received
svr:listen (80, connect)