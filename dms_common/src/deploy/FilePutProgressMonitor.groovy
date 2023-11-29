package deploy

import com.jcraft.jsch.SftpProgressMonitor
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.text.DecimalFormat

@CompileStatic
@Slf4j
class FilePutProgressMonitor extends TimerTask implements SftpProgressMonitor {
    private long progressInterval = 1 * 1000

    private boolean isEnd = false

    private long transferred

    private long fileSize

    private Timer timer

    private boolean isScheduled = false

    FilePutProgressMonitor(long fileSize, long progressInterval = 2000) {
        this.fileSize = fileSize
        this.progressInterval = progressInterval
    }

    @Override
    void run() {
        if (!isEnd()) {
            log.info("Transfer is in progress.")
            long transferred = getTransferred()
            if (transferred != fileSize) {
                log.info("Current transferred: " + transferred + " bytes")
                sendProgressMessage(transferred)
            } else {
                log.info("File transfer is done.")
                setEnd(true)
            }
        } else {
            log.info("Transfer done. Cancel timer.")
            stop()
        }
    }

    void stop() {
        log.info("Try to stop progress monitor.")
        if (timer != null) {
            timer.cancel()
            timer.purge()
            timer = null
            isScheduled = false
        }
        log.info("Progress monitor stopped.")
    }

    void start() {
        log.info("Try to start progress monitor.")
        if (timer == null) {
            timer = new Timer()
        }
        timer.schedule(this, 1000, progressInterval)
        isScheduled = true
        log.info("Progress monitor started.")
    }

    /**
     * print process info
     * @param transferred
     */
    private void sendProgressMessage(long transferred) {
        if (fileSize != 0) {
            double d = ((double) transferred * 100) / (double) fileSize
            DecimalFormat df = new DecimalFormat("#.##")
            log.info("Sending progress message: " + df.format(d) + "%")
        } else {
            log.info("Sending progress message: " + transferred)
        }
    }


    boolean count(long count) {
        if (isEnd()) return false
        if (!isScheduled) {
            start()
        }
        add(count)
        return true
    }


    void end() {
        setEnd(true)
        log.info("Transfer end.")
    }

    private synchronized void add(long count) {
        this.transferred = this.transferred + count
    }

    private synchronized long getTransferred() {
        return this.transferred
    }

    private synchronized void setEnd(boolean isEnd) {
        this.isEnd = isEnd
    }

    private synchronized boolean isEnd() {
        return isEnd
    }

    void init(int op, String src, String dest, long max) {
        // Not used for putting InputStream
    }
}