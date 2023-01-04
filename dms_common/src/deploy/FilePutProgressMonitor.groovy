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

    private long transfered

    private long fileSize

    private Timer timer

    private boolean isScheduled = false

    FilePutProgressMonitor(long fileSize, long progressInterval = 2000) {
        this.fileSize = fileSize
    }

    @Override
    void run() {
        if (!isEnd()) {
            log.info("Transfer is in progress.")
            long transfered = getTransfered()
            if (transfered != fileSize) {
                log.info("Current transfered: " + transfered + " bytes")
                sendProgressMessage(transfered)
            } else {
                log.info("File transfer is done.")
                setEnd(true)
            }
        } else {
            log.info("Transfer done. Cancel timer.")
            stop()
            return
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
     * @param transfered
     */
    private void sendProgressMessage(long transfered) {
        if (fileSize != 0) {
            double d = ((double) transfered * 100) / (double) fileSize
            DecimalFormat df = new DecimalFormat("#.##")
            log.info("Sending progress message: " + df.format(d) + "%")
        } else {
            log.info("Sending progress message: " + transfered)
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
        transfered = transfered + count
    }

    private synchronized long getTransfered() {
        return transfered
    }

    synchronized void setTransfered(long transfered) {
        this.transfered = transfered
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