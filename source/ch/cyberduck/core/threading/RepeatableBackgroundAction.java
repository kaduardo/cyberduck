package ch.cyberduck.core.threading;

/*
 * Copyright (c) 2002-2010 David Kocher. All rights reserved.
 *
 * http://cyberduck.ch/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Bug fixes, suggestions and comments should be sent to:
 * dkocher@cyberduck.ch
 */

import ch.cyberduck.core.Collection;
import ch.cyberduck.core.ConnectionCanceledException;
import ch.cyberduck.core.ErrorListener;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.ReachabilityFactory;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.TranscriptListener;
import ch.cyberduck.core.i18n.Locale;
import ch.cyberduck.ui.growl.Growl;

import org.apache.log4j.Logger;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * @version $Id: RepeatableBackgroundAction.java 10696 2012-12-21 14:41:05Z dkocher $
 */
public abstract class RepeatableBackgroundAction extends AbstractBackgroundAction<Boolean>
        implements ErrorListener, TranscriptListener {
    private static Logger log = Logger.getLogger(RepeatableBackgroundAction.class);
    private static final String lineSeparator = System.getProperty("line.separator");

    /**
     * Contains all exceptions thrown while this action was running
     */
    private List<BackgroundException> exceptions
            = new Collection<BackgroundException>();

    /**
     * This action encountered one or more exceptions
     */
    private boolean failed;

    /**
     * Contains the transcript of the session while this action was running
     */
    private StringBuilder transcript;

    /**
     *
     */
    private final int repeatAttempts
            = Preferences.instance().getInteger("connection.retry");

    /**
     * The number of times this action has been run
     */
    protected int repeatCount;

    /**
     * Maximum transcript buffer
     */
    private static final int TRANSCRIPT_MAX_LENGTH =
            Preferences.instance().getInteger("transcript.length");

    /**
     * @param exception Service error
     * @see ch.cyberduck.core.ErrorListener
     */
    @Override
    public void error(final BackgroundException exception) {
        // Do not report an error when the action was canceled intentionally
        Throwable cause = exception.getCause();
        if(cause instanceof ConnectionCanceledException) {
            log.warn(cause.getMessage());
            // Do not report as failed if instanceof ConnectionCanceledException
            return;
        }
        if(cause instanceof CertificateException) {
            log.warn(cause.getMessage());
            // Server certificate not accepted
            return;
        }
        if(cause instanceof SSLPeerUnverifiedException) {
            log.warn(cause.getMessage());
            // Server certificate not accepted
            return;
        }
        final String description
                = (null == exception.getPath()) ? exception.getHost().getHostname() : exception.getPath().getName();
        if(exceptions.size() < Preferences.instance().getInteger("growl.limit")) {
            Growl.instance().notify(exception.getMessage(), description);
        }
        if(!exceptions.contains(exception)) {
            exceptions.add(exception);
        }
        failed = true;
    }

    public List<BackgroundException> getExceptions() {
        return exceptions;
    }

    public String getTranscript() {
        return transcript.toString();
    }

    public boolean hasTranscript() {
        return transcript.length() > 0;
    }

    /**
     * Apppend to the transcript. Reset if maximum length has been reached.
     *
     * @param request Message was sent to the server
     * @param message @see ch.cyberduck.core.TranscriptListener
     * @see #TRANSCRIPT_MAX_LENGTH
     */
    @Override
    public void log(boolean request, String message) {
        if(transcript.length() > TRANSCRIPT_MAX_LENGTH) {
            transcript = new StringBuilder();
        }
        transcript.append(message).append(lineSeparator);
    }

    @Override
    public void init() {
        // Add to the registry so it will be displayed in the activity window.
        BackgroundActionRegistry.global().add(this);
    }

    @Override
    public boolean prepare() {
        for(Session session : this.getSessions()) {
            if(session != null) {
                session.addErrorListener(this);
                session.addTranscriptListener(this);
            }
        }
        // Clear the transcript and exceptions
        transcript = new StringBuilder();
        return super.prepare();
    }

    /**
     * To be overridden in concrete subclass
     *
     * @return The session if any or null if invalid
     */
    protected abstract List<Session> getSessions();

    /**
     * The number of times a new connection attempt should be made. Takes into
     * account the number of times already tried.
     *
     * @return Greater than zero if a failed action should be repeated again
     */
    public int retry() {
        if(!this.isCanceled()) {
            for(BackgroundException e : exceptions) {
                final Throwable cause = e.getCause();
                // Check for an exception we consider possibly temporary
                if(cause instanceof SocketException
                        || cause instanceof SocketTimeoutException
                        || cause instanceof UnknownHostException) {
                    // The initial connection attempt does not count
                    return repeatAttempts - repeatCount;
                }
            }
        }
        return 0;
    }

    protected void reset() {
        failed = false;
        exceptions.clear();
    }

    protected void diagnose() {
        List<BackgroundException> exceptions = this.getExceptions();
        final Host host = exceptions.get(exceptions.size() - 1).getHost();
        ReachabilityFactory.get().diagnose(host);
    }

    /**
     * @return True if the the action had a permanent failures. Returns false if
     *         there were only temporary exceptions and the action suceeded upon retry
     * @see #retry()
     */
    protected boolean hasFailed() {
        return failed;
    }

    public boolean isNetworkFailure() {
        for(BackgroundException e : this.getExceptions()) {
            final Throwable cause = e.getCause();
            if(cause instanceof SocketException || cause instanceof UnknownHostException) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void finish() {
        while(this.hasFailed() && this.retry() > 0) {
            if(log.isInfoEnabled()) {
                log.info(String.format("Retry failed background action %s", this));
            }
            // This is an automated retry. Wait some time first.
            this.pause();
            if(!this.isCanceled()) {
                repeatCount++;
                // Reset the failure status but remember the previous exception for automatic retry.
                failed = false;
                // Re-run the action with the previous lock used
                this.run();
            }
        }
        for(Session session : this.getSessions()) {
            if(session != null) {
                // It is important _not_ to do this in #cleanup as otherwise
                // the listeners are still registered when the next BackgroundAction
                // is already running
                session.removeTranscriptListener(this);
                session.removeErrorListener(this);
            }
        }
        super.finish();
    }

    /**
     * Idle this action for some time. Blocks the caller.
     */
    public void pause() {
        if(0 == Preferences.instance().getInteger("connection.retry.delay")) {
            log.info("No pause between retry");
            return;
        }
        final Timer wakeup = new Timer();
        final CyclicBarrier wait = new CyclicBarrier(2);
        wakeup.scheduleAtFixedRate(new TimerTask() {
            /**
             * The delay to wait before execution of the action in seconds
             */
            private int delay = (int) Preferences.instance().getDouble("connection.retry.delay");

            private final String pattern = Locale.localizedString("Retry again in {0} seconds ({1} more attempts)", "Status");

            @Override
            public void run() {
                if(0 == delay || RepeatableBackgroundAction.this.isCanceled()) {
                    // Cancel the timer repetition
                    this.cancel();
                    return;
                }
                for(Session session : getSessions()) {
                    if(session != null) {
                        session.message(MessageFormat.format(pattern, delay--, RepeatableBackgroundAction.this.retry()));
                    }
                }
            }

            @Override
            public boolean cancel() {
                try {
                    // Notifiy to return to caller from #pause()
                    wait.await();
                }
                catch(InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
                catch(BrokenBarrierException e) {
                    log.error(e.getMessage(), e);
                }
                return super.cancel();
            }
        }, 0, 1000); // Schedule for immediate execusion with an interval of 1s
        try {
            // Wait for notify from wakeup timer
            wait.await();
        }
        catch(InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        catch(BrokenBarrierException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * @return The session instance
     */
    @Override
    public Object lock() {
        return this.getSessions().iterator().next();
    }

    @Override
    public String toString() {
        for(Session session : this.getSessions()) {
            if(session != null) {
                return session.getHost().getHostname();
            }
        }
        return Locale.localizedString("Unknown");
    }
}
