package org.sade.servlet

import javax.servlet.{FilterConfig, FilterChain, ServletResponse, ServletRequest}
import org.squeryl.PrimitiveTypeMode

class TransactionFilter extends javax.servlet.Filter with PrimitiveTypeMode {
  def init(filterConfig: FilterConfig) {}

  def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    inTransaction {
      chain.doFilter(request, response)
    }
  }

  def destroy() {}
}