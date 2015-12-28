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

local gpio1Value = gpio.LOW
function toggleGPIO1LED()
    if gpio1Value == gpio.LOW then
        gpio1Value = gpio.HIGH
    else
        gpio1Value = gpio.LOW
    end
    gpio.write(gpio1, gpio1Value)
end

function writeConfig(ssid, pwd, ip, mask, gateway, passcode)
	file.open("config", "w+")
	file.writeline(ssid)
	if(pwd ~= nil) then
		file.writeline(pwd)
	else
		file.writeline("")
	end
	file.writeline(ip)
	file.writeline(mask)
	file.writeline(gateway)
	if(passcode ~= nil) then
		file.writeline(passcode)
	end
	file.flush()
	file.close()
	collectgarbage()
end