local wifiConfig = {}
local pin = 10
local value = gpio.LOW
function toggleLED()
    if value == gpio.LOW then
        value = gpio.HIGH
    else
        value = gpio.LOW
    end
    gpio.write(pin, value)
end
gpio.mode(pin, gpio.OUTPUT)
gpio.write(pin, value)
tmr.alarm(0, 1000, 1, toggleLED)

function urldecode(s)
  s = s:gsub('+', ' ')
       :gsub('%%(%x%x)', function(h)
							return string.char(tonumber(h, 16))
						 end)
  return s
end

function parsevars(s)
  local ans = {}
  for k,v in s:gmatch('([^&=?]-)=([^&=?]+)' ) do
    ans[ k ] = urldecode(v)
  end
  return ans
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
        cn:send("HTTP/1.1 200/OK\r\nServer: NodeLuau\r\nContent-Type: text/html\r\n\r\n")
		
		if(_GET.ssid ~= nil) then
			file.open("config", "w+")
			file.writeline(_GET.ssid)
			file.writeline(_GET.pwd)
			file.writeline(_GET.ip)
			file.writeline(_GET.mask)
			file.writeline(_GET.gateway)
			file.flush()
			file.close()
			buf = buf.."1"
			cn:send(buf);
			cn:close()
			collectgarbage()
			node.restart()
		else
			buf = buf.."<h1> ESP8266 Web Server Config</h1>"
			buf = buf.."<p>Chip needs configuration, use app.</p>"
			cn:send(buf);
			cn:close()
			collectgarbage()
		end
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