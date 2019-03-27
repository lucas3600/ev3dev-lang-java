package ev3dev.actuators.ev3;

import ev3dev.hardware.EV3DevDevice;
import ev3dev.hardware.EV3DevPlatform;
import ev3dev.utils.Sysfs;
import lejos.hardware.LED;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EV3Led extends EV3DevDevice implements LED {

    /**
     * Directions of the LED.
     */
    public enum Direction {
        LEFT,
        RIGHT
    }

    private static final Logger log = LoggerFactory.getLogger(EV3Led.class);

    public static final int LEFT = 0;
    public static final int RIGHT = 1;

    private final Direction direction;

    private final String LED_RED;
    private final String LED_GREEN;

    /**
     * Create an EV3LED object associated with the LED of the specified direction.
     *
     * @param direction the direction of the LED, either {@link Direction#LEFT} or {@link Direction#RIGHT}.
     * @throws RuntimeException if LED feature is not supported on the current platform.
     */
    public EV3Led(final Direction direction) {
        checkPlatform();

        if (direction == null) {
            log.error("You are not specifying any button.");
            throw new IllegalArgumentException("You are not specifying any button.");
        }

        this.direction = direction;

        if (direction == Direction.LEFT) {
            LED_RED = ev3DevProperties.getProperty("ev3.led.left.red");
            LED_GREEN = ev3DevProperties.getProperty("ev3.led.left.green");
        } else {
            LED_RED = ev3DevProperties.getProperty("ev3.led.right.red");
            LED_GREEN = ev3DevProperties.getProperty("ev3.led.right.green");
        }
    }

    /**
     * Creates an EV3LED object associated with the LED of the specified direction.
     *
     * @param button the direction of the LED, should be either {@link #LEFT} or {@link #RIGHT}.
     * @throws RuntimeException if LED feature is not supported on the current platform.
     * @deprecated Use {@link #EV3Led(Direction)} instead.
     */
    public EV3Led(final int button) {
        checkPlatform();

        if (button == LEFT) {
            LED_RED = ev3DevProperties.getProperty("ev3.led.left.red");
            LED_GREEN = ev3DevProperties.getProperty("ev3.led.left.green");
            direction = Direction.LEFT;
        } else if (button == RIGHT) {
            LED_RED = ev3DevProperties.getProperty("ev3.led.right.red");
            LED_GREEN = ev3DevProperties.getProperty("ev3.led.right.green");
            direction = Direction.RIGHT;
        } else {
            log.error("You are not specifying any button.");
            throw new IllegalArgumentException("You are not specifying any button.");
        }

    }

    /**
     * Checks if the current platform support this feature.
     *
     * @throws RuntimeException if this feature is not supported on the current platform.
     */
    private void checkPlatform() {
        if (!CURRENT_PLATFORM.equals(EV3DevPlatform.EV3BRICK)) {
            log.error("This actuator is specific of: {}", EV3DevPlatform.EV3BRICK);
            throw new RuntimeException("This actuator is specific of: " + EV3DevPlatform.EV3BRICK);
        }
    }

    //TODO Add Enums for patterns

    /**
     * 0: turn off button lights
     * 1/2/3: static light green/red/yellow
     * 4/5/6: normal blinking light green/red/yellow
     * 7/8/9: fast blinking light green/red/yellow
     * >9: same as 9.
     *
     * @param pattern
     */
    @Override
    public void setPattern(final int pattern) {
        //Off
        if (pattern == 0) {
            Sysfs.writeInteger(LED_RED, 0);
            Sysfs.writeInteger(LED_GREEN, 0);
        } else if (pattern == 1) {
            Sysfs.writeInteger(LED_RED, 0);
            Sysfs.writeInteger(LED_GREEN, 255);
        } else if (pattern == 2) {
            Sysfs.writeInteger(LED_RED, 255);
            Sysfs.writeInteger(LED_GREEN, 0);
        } else if (pattern == 3) {
            Sysfs.writeInteger(LED_RED, 255);
            Sysfs.writeInteger(LED_GREEN, 255);
        } else if (pattern > 3) {
            log.debug("This feature is not implemented");
        }
    }

    public Direction getDirection() {
        return direction;
    }
}