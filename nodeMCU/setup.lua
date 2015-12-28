local wifiConfig = {}

if(gpio.read(3) ~= 0) then
	gpio.mode(gpio1, gpio.OUTPUT)
	gpio.write(gpio1, gpio.LOW)
	tmr.alarm(5, 1000, 1, toggleGPIO1LED)
end

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
        if (vars ~= nil) then
            _GET = parsevars(vars)
        end
		if(_GET.ssid ~= nil) then
			writeConfig(_GET.ssid, _GET.pwd, _GET.ip, _GET.mask, _GET.gateway, _GET.passcode)
			buf = buf .. "HTTP/1.1 200 OK\r\nServer: WiFi Relay\r\nContent-Type: text/plain\r\n\r\n"
			buf = buf.."1"
			cn:send(buf);
			cn:close()
		else
			buf = buf .. "HTTP/1.1 200 OK\r\nServer: WiFi Relay\r\nContent-Type: text/html\r\n\r\n<html>"
			buf = buf.."<h1> ESP8266 Web Server Config</h1>"
			buf = buf.."<p>Chip needs configuration, use app.</p></html>"
			cn:send(buf);
			cn:close()
		end
		if(_GET.restart == "1") then
			tmr.alarm(2, 2000, 0, function()
				node.restart()
			end)
		end
		collectgarbage()
    end)
end

wifiConfig.ssid="ESP Relay "..node.chipid()
wifiConfig.pwd="12345678"

wifi.setmode(wifi.SOFTAP)
wifi.ap.config(wifiConfig)

tmr.alarm(1, 800, 1, function()
	if wifi.ap.getip() == nil then
		print("Waiting for AP setup")
	else
		tmr.stop(1)
		print("IP: " .. wifi.ap.getip())
		print('AP MAC: ',wifi.ap.getmac())
		local svr = net.createServer(net.TCP, 30)
		svr:listen(80, connect)
	end
end)