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