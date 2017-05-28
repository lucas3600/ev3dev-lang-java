package ev3dev.actuators.ev3.motors;

import ev3dev.hardware.EV3DevMotorDevice;
import ev3dev.hardware.EV3DevPlatforms;
import ev3dev.sensors.Battery;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.RegulatedMotorListener;
import lejos.utility.Delay;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstraction for a Regulated motors motors.
 * The basic control methods are:
 *  <code>forward, backward, reverseDirection, stop</code>
 * and <code>flt</code>. To set each motors's velocity, use {@link #setSpeed(int)
 * <code>setSpeed  </code> }.
 * The maximum velocity of the motors is limited by the battery voltage and load.
 * With no load, the maximum degrees per second is about 100 times the voltage
 * (for the large EV3 motors).  <br>
 * The velocity is regulated by comparing the tacho count with velocity times elapsed
 * time, and adjusting motors power to keep these closely matched. Changes in velocity
 * will be made at the rate specified via the
 * <code> setAcceleration(int acceleration)</code> method.
 * The methods <code>rotate(int angle) </code> and <code>rotateTo(int ange)</code>
 * use the tachometer to control the position at which the motors stops, usually within 1 degree
 * or 2.<br>
 *  <br> <b> Listeners.</b>  An object implementing the {@link lejos.robotics
 * <code> RegulatedMotorListener </code> } interface  may register with this class.
 * It will be informed each time the motors starts or stops.
 * <br> <b>Stall detection</b> If a stall is detected or if for some other reason
 * the speed regulation fails, the motors will stop, and
 * <code>isStalled()</code >  returns <b>true</b>.
 * <br>Motors will hold their position when stopped. If this is not what you require use
 * the flt() method instead of stop().
 *
 * @author Roger Glassey
 * @author Andy Shaw
 * @author Juan Antonio Breña Moral
 */
public @Slf4j abstract class BaseRegulatedMotor extends EV3DevMotorDevice implements RegulatedMotor, EV3DevBaseMotor {

    // Following should be set to the max SPEED (in deg/sec) of the motor when free running and powered by 9V
    protected final int MAX_SPEED_AT_9V;

    private int speed = 360;
    protected int acceleration = 6000;

    private boolean regulationFlag = true;

    private final List<RegulatedMotorListener> listenerList = Collections.synchronizedList(new ArrayList());

    /**
     * Constructor
     * @param motorPort motor port
     * @param moveP moveP
     * @param moveI moveI
     * @param moveD moveD
     * @param holdP holdP
     * @param holdI holdI
     * @param holdD holdD
     * @param offset offset
     * @param maxSpeed maxSpeed
     */
    public BaseRegulatedMotor(final String motorPort, float moveP, float moveI, float moveD,
			float holdP, float holdI, float holdD, int offset, int maxSpeed) {

        MAX_SPEED_AT_9V = maxSpeed;
        final String port = this.getMotorPort(motorPort);

        log.debug("Detecting motor on port: {}", port);
        this.detect(LEGO_PORT, port);
        log.debug("Setting port in mode: {}", TACHO_MOTOR);
        this.setStringAttribute(MODE, TACHO_MOTOR);
        Delay.msDelay(500);
        this.detect(TACHO_MOTOR, port);
        this.setStringAttribute(COMMAND, RESET);
	}
    
    /**
     * Removes this motors from the motors regulation system. After this call
     * the motors will be in float mode and will have stopped. Note calling any
     * of the high level move operations (forward, rotate etc.), will
     * automatically enable regulation.
     * @return true iff regulation has been suspended.
     */
    public boolean suspendRegulation() {
		this.regulationFlag = false;
        return this.regulationFlag;
    }

    /**
     * @return the current tachometer count.
     * @see lejos.robotics.RegulatedMotor#getTachoCount()
     */
    public int getTachoCount() {
    	return getIntegerAttribute(POSITION);
    }

    /**
     * Returns the current position that the motors regulator is trying to
     * maintain. Normally this will be the actual position of the motors and will
     * be the same as the value returned by getTachoCount(). However in some
     * circumstances (activeMotors that are in the process of stalling, or activeMotors
     * that have been forced out of position), the two values may differ. Note that
     * if regulation has been suspended calling this method will restart it.
     * @return the current position calculated by the regulator.
     */
    public float getPosition() {
        return 0.0f; //reg.getPosition();
    }

    @Override
    public void forward() {
        this.setStringAttribute(POLARITY, POLARITY_NORMAL);
        this.setSpeed(this.speed);
        if (!this.regulationFlag) {
            this.setStringAttribute(COMMAND, RUN_DIRECT);
        } else {
            this.setStringAttribute(COMMAND, RUN_FOREVER);
        }

        for (RegulatedMotorListener listener : listenerList) {
            listener.rotationStarted(this, this.getTachoCount(), this.isStalled(), System.currentTimeMillis());
        }
    }

    @Override
    public void backward(){
        this.setStringAttribute(POLARITY, POLARITY_INVERSED);
        this.setSpeed(this.speed);
        if (!this.regulationFlag) {
            this.setStringAttribute(COMMAND, RUN_DIRECT);
        } else {
            this.setStringAttribute(COMMAND, RUN_FOREVER);
        }

        for (RegulatedMotorListener listener : listenerList) {
            listener.rotationStarted(this, this.getTachoCount(), this.isStalled(), System.currentTimeMillis());
        }
    }

    /**
     * Set the motors into float mode. This will stop the motors without braking
     * and the position of the motors will not be maintained.
     */
    @Override
    public void flt(boolean b) {
        this.flt();
    }

    @Override
    public void flt() {
        log.debug("Not implemented");
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void coast() {
        this.setStringAttribute(STOP_COMMAND, COAST);
    }

    /**
     * Removes power from the motor and creates a passive electrical load.
     * This is usually done by shorting the motor terminals together.
     * This load will absorb the energy from the rotation of the motors and cause
     * the motor to stop more quickly than coasting.
     */
    public void brake() {
        if(this.getPlatform().equals(EV3DevPlatforms.EV3BRICK)) {
            this.setStringAttribute(STOP_COMMAND, BRAKE);
        }else{
            log.warn("This method is disabled for {} & {}", EV3DevPlatforms.PISTORMS, EV3DevPlatforms.BRICKPI);
        }
    }

    /**
     * Causes the motor to actively try to hold the current position.
     * If an external force tries to turn the motor, the motor will “push back” to maintain its position.
     */
    @Override
    public void hold() {
        if(this.getPlatform().equals(EV3DevPlatforms.EV3BRICK)) {
            this.setStringAttribute(STOP_COMMAND, HOLD);
        }else{
            log.warn("This method is disabled for {} & {}", EV3DevPlatforms.PISTORMS, EV3DevPlatforms.BRICKPI);
        }
    }

    /**
     * Causes motors to stop, pretty much
     * instantaneously. In other words, the
     * motors doesn't just stop; it will resist
     * any further motion.
     * Cancels any rotate() orders in progress
     */
    public void stop() {
		this.setStringAttribute(COMMAND, STOP);

        for (RegulatedMotorListener listener : listenerList) {
            listener.rotationStopped(this, this.getTachoCount(), this.isStalled(), System.currentTimeMillis());
        }
    }

    @Override
    public void stop(boolean b) {
        this.setStringAttribute(COMMAND, STOP);
    }

    /**
     * This method returns <b>true </b> if the motors is attempting to rotate.
     * The return value may not correspond to the actual motors movement.<br>
     * For example,  If the motors is stalled, isMoving()  will return <b> true. </b><br>
     * After flt() is called, this method will return  <b>false</b> even though the motors
     * axle may continue to rotate by inertia.
     * If the motors is stalled, isMoving()  will return <b> true. </b> . A stall can
     * be detected  by calling {@link #isStalled()};
     * @return true iff the motors is attempting to rotate.<br>
     */
    @Override
    public boolean isMoving() {
		return (this.getStringAttribute(STATE).contains(STATE_RUNNING));
    }

    /**
     * Sets desired motors speed , in degrees per second;
     * The maximum reliably sustainable velocity is  100 x battery voltage under
     * moderate load, such as a direct drive robot on the level.
     * @param speed value in degrees/sec
     */
    public void setSpeed(int speed) {
        this.speed = speed;
		if (!this.regulationFlag) {
            this.setIntegerAttribute(DUTY_CYCLE, speed);
		} else {
            this.setIntegerAttribute(SPEED, speed);
        }
    }

    /**
     * Reset the tachometer associated with this motors. Note calling this method
     * will cause any current move operation to be halted.
     */
    public void resetTachoCount() {
		this.setStringAttribute(COMMAND, RESET);
        this.regulationFlag = true;
    }
    
    /**
     * Rotate by the request number of degrees.
     * @param angle number of degrees to rotate relative to the current position
     * @param immediateReturn if true do not wait for the move to complete
     * Rotate by the requested number of degrees. Wait for the move to complete.
     */
    public void rotate(int angle, boolean immediateReturn) {
		this.setIntegerAttribute(POSITION_SP, angle);
		this.setStringAttribute(COMMAND, RUN_TO_REL_POS);
		
		if (!immediateReturn) {
			while(this.isMoving()){
			   // do stuff or do nothing
			   // possibly sleep for some short interval to not block
			}
		}

        for (RegulatedMotorListener listener : listenerList) {
            listener.rotationStarted(this, this.getTachoCount(), this.isStalled(), System.currentTimeMillis());
        }
    }

    /**
     * Rotate by the requested number of degrees. Wait for the move to complete.
     * @param angle angle
     */
    public void rotate(int angle) {
        rotate(angle, false);
    }

    public void rotateTo(int limitAngle, boolean immediateReturn) {
    	this.setIntegerAttribute(POSITION_SP, limitAngle);
    	this.setStringAttribute(COMMAND, RUN_TO_ABS_POS);
		
		if (!immediateReturn) {
			while(this.isMoving()){
			    // do stuff or do nothing
			    // possibly sleep for some short interval to not block
			}
		}

        for (RegulatedMotorListener listener : listenerList) {
            listener.rotationStarted(this, this.getTachoCount(), this.isStalled(), System.currentTimeMillis());
        }
    }
    
    /**
     * Rotate to the target angle. Do not return until the move is complete.
     * @param limitAngle Angle to rotate to.
     */
    public void rotateTo(int limitAngle) {
    	rotateTo(limitAngle, false);
    }

    /**
     * Return the current target speed.
     * @return the current target speed.
     */
    public int getSpeed() {
		if(!this.regulationFlag) {
            return this.getIntegerAttribute(DUTY_CYCLE);
		}else {
            return this.getIntegerAttribute(SPEED);
        }

    }

    /**
     * Return true if the motors is currently stalled.
     * @return true if the motors is stalled, else false
     */
    public boolean isStalled() {
		return (this.getStringAttribute(STATE).contains(STATE_STALLED));
    }
    
    /**
     * Return the current velocity.
     * @return current velocity in degrees/s
     */
    public int getRotationSpeed() {
        return 0;//Math.round(reg.getCurrentVelocity());
    }

    @Override
    public void addListener(RegulatedMotorListener regulatedMotorListener) {
        listenerList.add(regulatedMotorListener);
    }

    @Override
    public RegulatedMotorListener removeListener() {
        if(listenerList.size() > 0){
            listenerList.remove(listenerList.size() - 1);
        }
        return null;
    }

    @Override
    public void waitComplete() {
        log.debug("Not implemented");
        throw new RuntimeException("Not implemented");
    }

    @Override
    public float getMaxSpeed() {
        // It is generally assumed, that the maximum accurate speed of an EV3 Motor is
        // 100 degree/second * Voltage. We generalise this to other LEGO motors by returning a value
        // that is based on 90% of the maximum free running speed of the motor.
        // TODO: Should this be using the Brick interface?
        // TODO: If we ever allow the regulator class be remote, then we will need to ensure we
        // get the voltage of the remote brick not the local one.
        //return LocalEV3.ev3.getPower().getVoltage() * MAX_SPEED_AT_9V/9.0f * 0.9f;
        return Battery.getInstance().getVoltage() * MAX_SPEED_AT_9V/9.0f * 0.9f;
    }

    @Override
    public void setAcceleration(int i) {
        log.debug("Not implemented");
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void synchronizeWith(RegulatedMotor[] regulatedMotors) {
        log.debug("Not implemented");
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void startSynchronization() {
        log.warn("Not implemented the method: startSynchronization");

        //throw new RuntimeException("Not implemented");
    }

    @Override
    public void endSynchronization() {
        log.warn("Not implemented the method: endSynchronization");

        //throw new RuntimeException("Not implemented");
    }

}
